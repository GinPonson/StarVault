package com.starvault.data.remote.cloud115

import com.starvault.data.local.auth.OpenAuthStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Token401Interceptor 行为测试 — 对齐 OpenList 115-sdk-go authRequest 自动 refresh 语义。
 *
 * 触发条件(对齐 115-sdk-go request.go:42 `Is401Started || code == 99`):
 *  - 任何 401 开头业务码(40140123/40140124/40140126/40199xx/40100000)→ refresh
 *  - code == 99(通用鉴权错)→ refresh
 *  - 其它业务码 → 透传
 *
 * 用 mockk 模拟 OkHttp Chain/Response,不起 MockWebServer,直接验证 interceptor 行为。
 */
class Token401InterceptorTest {

    private lateinit var tokenStore: OpenAuthStore
    private lateinit var interceptor: Token401Interceptor

    private val oldAccessToken = "old_at_AAAA"
    private val oldRefreshToken = "old_rt_BBBB"
    private val newAccessToken = "new_at_CCCC"
    private val newRefreshToken = "new_rt_DDDD"

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.i(any(), any<String>()) } returns 0
        every { android.util.Log.d(any(), any<String>()) } returns 0

        tokenStore = mockk(relaxed = true)
        // 1st call: 进入 mutex 前读 → old
        // 2nd call: double-check 读 → 仍是 old(触发 refresh)
        // 3rd call: refresh 完后再读 → new(下次请求会用到)
        every { tokenStore.accessTokenBlocking() } returnsMany listOf(
            oldAccessToken, oldAccessToken, newAccessToken
        )
        coEvery { tokenStore.refreshToken() } returns oldRefreshToken

        // refresh API:用 mockk 替代 OpenAuthApiService(避开 MockWebServer)
        val refreshApi = mockk<OpenAuthApiService>(relaxed = true)
        coEvery { refreshApi.refreshToken(refreshToken = oldRefreshToken) } returns retrofit2.Response.success(
            com.starvault.data.remote.cloud115.ApiEnvelope(
                state = kotlinx.serialization.json.JsonPrimitive(1),
                data = kotlinx.serialization.json.JsonObject(mapOf(
                    "access_token" to kotlinx.serialization.json.JsonPrimitive(newAccessToken),
                    "refresh_token" to kotlinx.serialization.json.JsonPrimitive(newRefreshToken),
                    "expires_in" to kotlinx.serialization.json.JsonPrimitive(7200),
                )),
            )
        )
        interceptor = Token401Interceptor(
            tokenStore = tokenStore,
            refreshApi = refreshApi,
            mutex = Mutex(),
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    // ─── 核心场景 ─────────────────────────────────────────────────────

    @Test
    fun `non-401 response passes through without refresh`() = runBlocking {
        val resp = listFilesResp(
            code = 200,
            body = """{"state":true,"data":[],"count":0}""",
        )
        val chain = mockChain(resp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        assertEquals(resp, out)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `code 40140124 triggers refresh, saves new tokens, resends with new Bearer`() = runBlocking {
        val firstResp = listFilesResp(
            code = 200,
            body = """{"state":false,"code":40140124,"message":"签名校验失败","data":[]}""",
        )
        val retryResp = listFilesResp(
            code = 200,
            body = """{"state":true,"data":[],"count":0}""",
        )
        val chain = mockChain(firstResp, retryResp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        coVerify(exactly = 1) {
            tokenStore.saveRefreshedTokens(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = 7200L,
            )
        }
    }

    @Test
    fun `code 40140123 (PKCE format error) also triggers refresh`() = runBlocking {
        val firstResp = listFilesResp(200, """{"state":false,"code":40140123,"data":[]}""")
        val retryResp = listFilesResp(200, """{"state":true,"data":[],"count":0}""")
        val chain = mockChain(firstResp, retryResp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        coVerify(exactly = 1) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `code 40140126 (verify failed) also triggers refresh`() = runBlocking {
        val firstResp = listFilesResp(200, """{"state":false,"code":40140126,"data":[]}""")
        val retryResp = listFilesResp(200, """{"state":true,"data":[],"count":0}""")
        val chain = mockChain(firstResp, retryResp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        coVerify(exactly = 1) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `code 99 (generic auth error) also triggers refresh`() = runBlocking {
        val firstResp = listFilesResp(200, """{"state":false,"code":99,"message":"auth failed","data":[]}""")
        val retryResp = listFilesResp(200, """{"state":true,"data":[],"count":0}""")
        val chain = mockChain(firstResp, retryResp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        coVerify(exactly = 1) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `code 40100000 (generic 401) also triggers refresh`() = runBlocking {
        val firstResp = listFilesResp(200, """{"state":false,"code":40100000,"data":[]}""")
        val retryResp = listFilesResp(200, """{"state":true,"data":[],"count":0}""")
        val chain = mockChain(firstResp, retryResp)

        val out = interceptor.intercept(chain)

        assertEquals(200, out.code)
        coVerify(exactly = 1) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `40140124 with refresh_token null returns original response`() = runBlocking {
        coEvery { tokenStore.refreshToken() } returns null  // refresh_token 缺失

        val resp = listFilesResp(200, """{"state":false,"code":40140124,"data":[]}""")
        val chain = mockChain(resp)

        val out = interceptor.intercept(chain)
        assertEquals(200, out.code)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `40140124 with refresh biz failure returns original response`() = runBlocking {
        // refresh API 返回 state=false
        val refreshApi = mockk<OpenAuthApiService>(relaxed = true)
        coEvery { refreshApi.refreshToken(refreshToken = any()) } returns retrofit2.Response.success(
            com.starvault.data.remote.cloud115.ApiEnvelope(
                state = kotlinx.serialization.json.JsonPrimitive(0),
                message = "refresh token expired",
                data = null,
            )
        )
        val localInterceptor = Token401Interceptor(
            tokenStore = tokenStore,
            refreshApi = refreshApi,
            mutex = Mutex(),
        )

        val resp = listFilesResp(200, """{"state":false,"code":40140124,"data":[]}""")
        val chain = mockChain(resp)

        val out = localInterceptor.intercept(chain)
        assertEquals(200, out.code)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `40140124 with refresh HTTP error returns original response`() = runBlocking {
        val refreshApi = mockk<OpenAuthApiService>(relaxed = true)
        coEvery { refreshApi.refreshToken(refreshToken = any()) } returns retrofit2.Response.error(500, "boom".toResponseBody("text/plain".toMediaType()))
        val localInterceptor = Token401Interceptor(
            tokenStore = tokenStore,
            refreshApi = refreshApi,
            mutex = Mutex(),
        )

        val resp = listFilesResp(200, """{"state":false,"code":40140124,"data":[]}""")
        val chain = mockChain(resp)

        val out = localInterceptor.intercept(chain)
        assertEquals(200, out.code)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `retry header prevents second refresh on second 40140124`() = runBlocking {
        val firstResp = listFilesResp(200, """{"state":false,"code":40140124,"data":[]}""")
        val secondResp = listFilesResp(200, """{"state":false,"code":40140124,"data":[]}""")
        val chain = mockChain(firstResp, secondResp)

        val out = interceptor.intercept(chain)
        assertEquals(200, out.code)
        // refresh 只调 1 次(第二次 X-Token-Retry 阻止)
        coVerify(exactly = 1) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `non-401 500 error passes through without refresh`() = runBlocking {
        val resp = listFilesResp(500, """{"error":"server down"}""")
        val chain = mockChain(resp)

        val out = interceptor.intercept(chain)
        assertEquals(500, out.code)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    @Test
    fun `code 10001 (business error, not 401) passes through without refresh`() = runBlocking {
        // 业务码 10001(网络错/参数错),不是 401,不触发 refresh
        val resp = listFilesResp(200, """{"state":false,"code":10001,"message":"参数错误","data":[]}""")
        val chain = mockChain(resp)

        val out = interceptor.intercept(chain)
        assertEquals(200, out.code)
        coVerify(exactly = 0) { tokenStore.saveRefreshedTokens(any(), any(), any()) }
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private fun listFilesResp(code: Int, body: String): Response = Response.Builder()
        .request(Request.Builder().url("https://proapi.115.com/open/ufile/files?cid=0").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    /**
     * Mock Interceptor.Chain:
     *  - chain.request() 返固定 Request
     *  - chain.proceed(req) 第一次返 [first],第二次(proceed 重发)返 [second]
     */
    private fun mockChain(vararg proceedResults: Response): Interceptor.Chain {
        require(proceedResults.isNotEmpty())
        val req = Request.Builder().url("https://proapi.115.com/open/ufile/files?cid=0").build()
        val chain = mockk<Interceptor.Chain>(relaxed = true)
        every { chain.request() } returns req
        var idx = 0
        every { chain.proceed(any()) } answers {
            val pos = idx
            idx += 1
            proceedResults[pos]
        }
        return chain
    }
}
