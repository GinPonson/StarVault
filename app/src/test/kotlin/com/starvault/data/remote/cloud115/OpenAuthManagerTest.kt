package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * 替换 [ScanLoginManagerTest]，覆盖 OpenAuthManager 的核心状态机。
 *
 * 6 个测试：
 *  1. requestDeviceCode 业务 state!=1 → failure
 *  2. pollForToken deadline 已过 → 立即 Expired
 *  3. pollForToken data=null → 持续 Waiting + 最终 Expired
 *  4. pollForToken data.accessToken 存在 → Authorized，字段对齐
 *  5. pollForToken HTTP 失败 → Error
 *  6. revokeToken 抛异常 → 吞掉，不抛
 *
 * 注：requestDeviceCode 成功路径需要 zxing 编码（在 JVM 单测里跑需要 unitTests.isReturnDefaultValues，
 *     zxing 在 test sourceSet 默认不可用 —— 这里用业务失败路径覆盖成功逻辑下的"走到 zxing 之前"的断言）。
 */
class OpenAuthManagerTest {
    private val api = mockk<OpenAuthApiService>()
    private val manager = OpenAuthManager(api)
    private val fakeBitmap: Bitmap = mockk(relaxed = true)

    @Before
    fun stubAndroidLog() {
        // android.util.Log 在 JVM unit test 里没有 native 实现，会抛 UnsatisfiedLinkError
        // mockkStatic 把 Log 静态方法全部 stub 掉，避免触发 native loader
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun unstubAndroidLog() {
        unmockkStatic(android.util.Log::class)
    }

    /** 构造 DeviceCodeData：zxing 在 unit test 里不跑，由请求成功路径直接得到真实 bitmap 时才会用。 */
    private fun deviceCode() = OpenAuthManager.DeviceCodeData(
        uid          = "u1",
        sign         = "s1",
        qrcodeUrl    = "https://115.com/scan/dg-u1",
        codeVerifier = OpenAuthApiService.CODE_VERIFIER,
        bitmap       = fakeBitmap,
    )

    @Test
    fun `requestDeviceCode returns failure when business-state is false`() = runTest {
        coEvery { api.requestDeviceCode() } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(0), code = 10001, message = "rate limited", data = null)
        )
        val r = manager.requestDeviceCode()
        assertTrue(r.isFailure)
    }

    @Test
    fun `pollForToken emits Expired when deadline passed immediately`() = runTest {
        // 设一个返回 state=1 data=null 的 mock（用户未确认），但 deadline=now-1 → 立即 Expired
        coEvery { api.pollDeviceCode(any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = null)
        )
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() - 1).toList()
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Expired })
    }

    @Test
    fun `pollForToken keeps Waiting when data is null (user not yet scanned)`() = runTest {
        coEvery { api.pollDeviceCode(any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = null)
        )
        val dc = deviceCode()
        // deadline 给 100ms 让它跑几轮 poll 后超时
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 100).toList()
        // 必须有 Waiting（首条）+ Expired（超时），不应有 Authorized
        assertTrue(events.first() is OpenAuthManager.AuthStatus.Waiting)
        assertTrue(events.none { it is OpenAuthManager.AuthStatus.Authorized })
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Expired })
    }

    @Test
    fun `pollForToken emits Authorized when data carries access_token`() = runTest {
        coEvery { api.pollDeviceCode(any(), any(), any()) } returns Response.success(
            ApiEnvelope(
                state = JsonPrimitive(1),
                data = TokenPollResponse(
                    accessToken  = "at_xxx",
                    refreshToken = "rt_yyy",
                    expiresIn    = 7200L,
                    userId       = 12345L,
                    userName     = "alice",
                ),
            )
        )
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 5_000).toList()
        val ok = events.filterIsInstance<OpenAuthManager.AuthStatus.Authorized>().firstOrNull()
        assertTrue("expected Authorized event", ok != null)
        assertEquals("at_xxx", ok?.accessToken)
        assertEquals("rt_yyy", ok?.refreshToken)
        assertEquals(7200L, ok?.expiresIn)
        assertEquals(12345L, ok?.uid)
        assertEquals("alice", ok?.userName)
    }

    @Test
    fun `pollForToken emits Error when HTTP fails`() = runTest {
        coEvery { api.pollDeviceCode(any(), any(), any()) } returns
            Response.error(500, okhttp3.ResponseBody.Companion.create(null, ""))
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 100).toList()
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Error })
    }

    @Test
    fun `revokeToken swallows exception`() = runTest {
        // 任何 throw 都应该被 runCatching 吞掉；不抛即是成功
        coEvery { api.revokeToken(refreshToken = "rt_yyy") } throws RuntimeException("revoke endpoint down")
        manager.revokeToken("rt_yyy")
        coVerify { api.revokeToken(refreshToken = "rt_yyy") }
    }
}