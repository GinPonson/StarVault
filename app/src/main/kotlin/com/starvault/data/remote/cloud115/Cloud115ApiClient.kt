package com.starvault.data.remote.cloud115

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 115 网盘 HTTP 客户端工厂。
 *
 *  2 个 base URL（按用途分）：
 *  - OPEN_AUTH_BASE_URL : qrcodeapi.115.com/  (OAuth 设备码 + 状态轮询)
 *  - OPEN_API_BASE_URL  : proapi.115.com/     (OAuth Bearer 设计归属域:`/open/ufile/...`、`/open/folder/...`、`/open/video/...`、`/open/user/info`)
 *
 *  **不再有 webapi.115.com** — OAuth Bearer 迁移完成,所有端点已迁到 proapi / passportapi。
 *
 *  2 个 OkHttpClient（按超时策略分）：
 *  - 常规 30s client    : proapi + qrcodeapi POST 端点
 *  - long-poll 65s client: qrcodeapi /open/get/status/ 长轮询（115 会挂 30~60s）
 *  - 共用拦截器          : browser-like + AuthHeader（Bearer 注入）
 *
 *  Bearer token 由 [AuthHeaderInterceptor] 在请求时从 [com.starvault.data.local.auth.OpenAuthStore] 实时读。
 *
 *  使用方式：每个 base URL 一个 [Retrofit] 实例,OkHttpClient 按超时策略分两类。
 *
 *  替换历史：[CookieInterceptor] → [AuthHeaderInterceptor];[scanApiService] → [openAuthApiService] / [statusPollApiService];webapi /files → proapi /open/ufile/files。
 */
object Cloud115ApiClient {

    /** OAuth 设备码 + 状态轮询域（共享 baseUrl，2 个独立 Retrofit 接口）。 */
    const val OPEN_AUTH_BASE_URL = "https://qrcodeapi.115.com/"

    /**
     * proapi open 域（OAuth Bearer 鉴权）。
     *
     *  - `/open/ufile/...`   : 文件列表 / 搜索 / downurl(原图直链)
     *  - `/open/folder/...`  : get_info(单文件/夹详情)
     *  - `/open/video/play`  : 视频 m3u8(多清晰度)
     *  - `/open/user/info`   : 用户信息 + 空间概要 + VIP
     *  - `/open/upload/...`  : 上传(Phase 2)
     *
     * **这是 OAuth Bearer token 的合法域**（p115client/client.py:2573 / 4256 / 3100 + OpenListTeam/115-sdk-go/const.go）。
     * webapi.115.com 已废弃,OAuth Bearer 在那里不被接受。
     */
    const val OPEN_API_BASE_URL = "https://proapi.115.com/"

    /** 长轮询 read timeout：115 get/status/ 实测会挂 30~60s，留 5s buffer。 */
    private const val LONG_POLL_READ_TIMEOUT_SECONDS = 65L

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * 共用 Builder：注入浏览器伪装头 + Bearer 拦截器。不同超时在此基础上覆写。
     */
    private fun baseBuilder(tokenProvider: () -> String?): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .addInterceptor(browserLikeHeaderInterceptor())
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))

    /**
     * 构建常规 30s 超时 OkHttpClient（proapi + OAuth POST 端点）。
     *
     * @param tokenProvider 同步 lambda，从 [OpenAuthStore.accessTokenBlocking] 注入；
     *                       未登录时返回 null（不注入 Authorization 头）
     */
    fun buildOkHttpClient(tokenProvider: () -> String?): OkHttpClient =
        baseBuilder(tokenProvider)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * 构建长轮询 OkHttpClient：65s read timeout 覆盖 115 get/status/ 的 30~60s 持握。
     *
     * 其它超时（connect / write）维持 30s 即可。
     */
    fun buildLongPollOkHttpClient(tokenProvider: () -> String?): OkHttpClient =
        baseBuilder(tokenProvider)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(LONG_POLL_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * open 域 Retrofit 工厂（OAuth 设备码 [OpenAuthApiService]）。
     * 复用调用方传入的 client（30s 即可）。
     */
    fun openAuthRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_AUTH_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** open 域 [OpenAuthApiService] 工厂（独立 client，用于单元测试）。 */
    fun openAuthApiService(tokenProvider: () -> String?): OpenAuthApiService =
        openAuthRetrofit(buildOkHttpClient(tokenProvider)).create(OpenAuthApiService::class.java)

    /** open 域 [OpenAuthApiService] 工厂（共享 OkHttpClient，ServiceLocator 用）。 */
    fun openAuthApiService(client: OkHttpClient): OpenAuthApiService =
        openAuthRetrofit(client).create(OpenAuthApiService::class.java)

    /**
     * open 域长轮询 Retrofit 工厂（[StatusPollApi]）。
     * 必须传 long-poll client（65s read timeout）。
     */
    fun statusPollRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_AUTH_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** open 域长轮询 [StatusPollApi] 工厂（独立 client，用于单元测试）。 */
    fun statusPollApiService(tokenProvider: () -> String?): StatusPollApi =
        statusPollRetrofit(buildLongPollOkHttpClient(tokenProvider)).create(StatusPollApi::class.java)

    /** open 域长轮询 [StatusPollApi] 工厂（共享 long-poll client，ServiceLocator 用）。 */
    fun statusPollApiService(client: OkHttpClient): StatusPollApi =
        statusPollRetrofit(client).create(StatusPollApi::class.java)

    /**
     * proapi open 域 Retrofit 工厂。
     *
     * 共用 [okHttpClient]（30s 超时 + Bearer 注入 + 浏览器伪装头），
     * 仅替换 baseUrl 到 proapi.115.com。
     */
    fun openApiRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** proapi 域 [OpenFileApiService] 工厂（独立 client，用于单元测试）。 */
    fun openFileApiService(tokenProvider: () -> String?): OpenFileApiService =
        openApiRetrofit(buildOkHttpClient(tokenProvider)).create(OpenFileApiService::class.java)

    /** proapi 域 [OpenFileApiService] 工厂（共享 OkHttpClient）。 */
    fun openFileApiService(client: OkHttpClient): OpenFileApiService =
        openApiRetrofit(client).create(OpenFileApiService::class.java)

    /** proapi 域 [OpenUserApiService] 工厂（独立 client）。 */
    fun openUserApiService(tokenProvider: () -> String?): OpenUserApiService =
        openApiRetrofit(buildOkHttpClient(tokenProvider)).create(OpenUserApiService::class.java)

    /** proapi 域 [OpenUserApiService] 工厂（共享 OkHttpClient）。 */
    fun openUserApiService(client: OkHttpClient): OpenUserApiService =
        openApiRetrofit(client).create(OpenUserApiService::class.java)

    /**
     * 浏览器伪装头：Referer/Origin/User-Agent 全用 115 域名 + Android UA，
     * 否则 115 部分端点返回 state=false + 跨域拦截。
     *
     * 关键：UA = Android Chrome,**`/open/ufile/downurl` 必须传此 UA 才能拿到 CDN 直链签名**
     * （OpenListTeam/115-sdk-go/fs.go:309-319 + OpenList driver.go:144 `ua = base.UserAgent`）。
     *
     * 注：CDN 缩略图 URL（*.115cdn.com 等）依赖签名参数 + Referer/Origin/UA，
     * Bearer 多余但无害（[AuthHeaderInterceptor] 一并注入）。
     */
    private fun browserLikeHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
            .header("Accept", "application/json, text/plain, *")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .header("Referer", "https://115.com/")
            .header("Origin", "https://115.com")
            .build()
        chain.proceed(req)
    }
}