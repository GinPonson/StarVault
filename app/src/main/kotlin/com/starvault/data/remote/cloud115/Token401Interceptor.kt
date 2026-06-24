package com.starvault.data.remote.cloud115

import android.util.Log
import com.starvault.data.local.auth.OpenAuthStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自动 refresh 拦截器(对齐 OpenList 115-sdk-go authRequest 行为)。
 *
 * ## 触发条件
 *
 * 115 OAuth 的 access_token 过期(默认 2h)不会立即触发 HTTP 401,而是业务层返回
 * `state=false + code=401xxxx`("签名校验失败" / "格式错误" / "verify 失败" 等 —
 * p115client 错误码表,40140123/40140124/40140126 等)。需要在响应阶段识别并触发 refresh。
 *
 * 对齐 OpenList 115-sdk-go request.go:42 `Is401Started(code) || code == 99`:
 *  - **任何 401 开头**的业务码(40140123/40140124/40140126/40199xx/40100000)→ refresh
 *  - **code == 99** → refresh(通用鉴权错)
 *  - 其它业务码(state=true 或 非 401)→ 透传
 *
 * ## 流程
 *
 *  1. **peekBody** 8KB 不消耗响应流,JSON 解析出 `code` 字段
 *  2. 命中 → 拿 [Mutex] 串行化(避免 N 个并发请求各自 refresh)
 *  3. double-check:其它协程可能已 refresh 完,tokenStore 里的 at 已变 — 直接用
 *  4. 调 [OpenAuthApiService.refreshToken](**走独立 client,不带本拦截器,防递归**)
 *  5. 成功 → [OpenAuthStore.saveRefreshedTokens] 同步落库
 *  6. 用新 Bearer 重发原请求(加 `X-Token-Retry: 1` 防二次递归)
 *
 * ## 边界
 *
 *  - **refresh_token 也过期** → refresh 业务失败,返回原 401 resp,UI 报"登录失效",NavHost 跳 Login
 *  - **网络断** → refresh 抛异常,Mutex 释放,原 401 resp 返回,UI 报网络错
 *  - **重发后再 401** → `X-Token-Retry: 1` header 阻止递归,放弃(极罕见,refresh_token 已被服务端吊销)
 *
 * ## 同步阻塞
 *
 * OkHttp Interceptor 是同步接口。`runBlocking` + `Mutex.withLock` 会阻塞当前线程:
 *  - DataStore IO 1-10ms
 *  - refresh 网络 1-3s(115 远端)
 *  - 重发原请求 1-3s
 *
 * 阻塞线程 = API 调用所在的 OkHttp Dispatcher worker 线程(不是 Main 线程)。
 * 我们的 ViewModel 在 `viewModelScope`(Dispatchers.Main)发 API,Retrofit/Suspend 切到
 * OkHttp 的 worker 池后才进 interceptor;**主线程不会冻**。
 *
 * 参考:
 *  - OpenList 115-sdk-go utils.go:21-24 `Is401Started`
 *  - OpenList 115-sdk-go request.go:33-50 `authRequest` 自动 refresh
 *
 * @param tokenStore  DataStore 持久化(同步读 at/rt,suspend 写新 token)
 * @param refreshApi  refresh 端点专用 API(必须走不带本 interceptor 的独立 client)
 * @param mutex       全局串行化,避免 N 请求同时 refresh(用 ServiceLocator 进程级实例)
 */
internal class Token401Interceptor(
    private val tokenStore: OpenAuthStore,
    private val refreshApi: OpenAuthApiService,
    private val mutex: Mutex,
) : Interceptor {

    companion object {
        private const val TAG = "Token401Interceptor"
        /** peekBody 字节上限;401 错误响应 < 200B,8KB 留 40x 余量。 */
        private const val PEEK_BYTES = 8 * 1024L
        /** 重发请求携带,第二次命中 401 时放弃(防递归 + 防死循环)。 */
        private const val RETRY_HEADER = "X-Token-Retry"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val resp = chain.proceed(req)

        // 401 / 5xx:业务层没拿到(走 HTTP 层)→ 不归本拦截器管,原样返回
        if (!resp.isSuccessful) return resp

        // 二次递归防护:重发后还 401 → 放弃
        if (req.header(RETRY_HEADER) != null) {
            return resp
        }

        // peekBody 不消耗响应流;只读 8KB,JSON 解析 `code` 字段
        val bodyString = resp.peekBody(PEEK_BYTES).string()
        val code = parseErrorCode(bodyString)
        if (!isExpiredCode(code)) {
            return resp
        }

        Log.w(TAG, "code=$code on ${req.url} → refresh access_token")

        val newToken = runBlocking {
            mutex.withLock {
                refreshAccessToken()
            }
        }

        if (newToken.isNullOrBlank()) {
            // refresh 失败:返回原 401 resp,UI 层报"登录失效"
            Log.w(TAG, "refresh failed, returning original $code resp")
            return resp
        }

        // 旧 resp 关闭,避免连接泄漏
        resp.close()

        // 用新 Bearer + Retry header 重发
        val retried = req.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header(RETRY_HEADER, "1")
            .build()
        return chain.proceed(retried)
    }

    /**
     * 在 mutex 内执行 refresh;返回新 access_token,失败返回 null。
     *
     * double-check:进入 mutex 后,先读一次 DataStore,如果和进入前已经不一样(其它协程刚
     * refresh 过),直接用新 token,不重新调 refresh API。
     */
    private suspend fun refreshAccessToken(): String? {
        // 1) double-check
        val tokenBefore = tokenStore.accessTokenBlocking()
        val tokenNow = tokenStore.accessTokenBlocking()
        if (tokenBefore != null && tokenBefore != tokenNow && !tokenNow.isNullOrBlank()) {
            Log.d(TAG, "double-check: token already refreshed by sibling, reuse")
            return tokenNow
        }

        // 2) 拿 refresh_token
        val rt = tokenStore.refreshToken()
        if (rt.isNullOrBlank()) {
            Log.w(TAG, "refresh_token is null/blank, cannot refresh")
            return null
        }

        // 3) 调 refresh API
        val refreshResp = try {
            refreshApi.refreshToken(refreshToken = rt)
        } catch (e: Throwable) {
            Log.e(TAG, "refresh API threw", e)
            return null
        }
        if (!refreshResp.isSuccessful) {
            Log.e(TAG, "refresh HTTP ${refreshResp.code()}")
            return null
        }
        val env = refreshResp.body() ?: return null
        if (!env.isOk) {
            Log.e(TAG, "refresh biz fail: ${env.message}")
            return null
        }
        // data 字段是 JsonElement(非 TokenRefreshResponse),因为 passportapi refresh 失败响应
        // data 是 [] 空数组,strict 类型不兼容。手动解析 JsonObject:
        val dataObj = env.data as? JsonObject ?: run {
            Log.e(TAG, "refresh data is not object: ${env.data}")
            return null
        }
        val newAt = dataObj["access_token"]?.jsonPrimitive?.content
        val newRt = dataObj["refresh_token"]?.jsonPrimitive?.content
        if (newAt.isNullOrBlank() || newRt.isNullOrBlank()) {
            Log.e(TAG, "refresh response missing access_token / refresh_token")
            return null
        }
        val expiresIn = dataObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 7200L

        // 4) 落库
        tokenStore.saveRefreshedTokens(
            accessToken = newAt,
            refreshToken = newRt,
            expiresIn = expiresIn,
        )
        Log.i(TAG, "refresh ok, new at=${newAt.take(8)}...")
        return newAt
    }

    /**
     * 从 peek 出来的 body 字符串解析 `code` 字段。
     *
     * 用 kotlinx-serialization 而不是 substring contains:
     *  - 不依赖 `code` 字段在 JSON 里的具体位置(避免误判)
     *  - 对 40140124 / 40140123 / 40140126 / 40100000 / 99 都能稳定提取
     *  - JSON 解析失败 → 返回 null(非 JSON 响应按"非 401"处理)
     */
    private fun parseErrorCode(body: String): Long? = try {
        Json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.content?.toLongOrNull()
    } catch (e: Throwable) {
        null
    }

    /**
     * 判断 [code] 是否应该触发 refresh(对齐 OpenList 115-sdk-go `Is401Started || code == 99`):
     *  - 任何 401 开头的码(40140123/40140124/40140126/40199xx/40100000)
     *  - code == 99(通用鉴权错)
     *  - null / 其它 → 不触发
     */
    private fun isExpiredCode(code: Long?): Boolean = code != null &&
        (code == 99L || code.toString().startsWith("401"))
}
