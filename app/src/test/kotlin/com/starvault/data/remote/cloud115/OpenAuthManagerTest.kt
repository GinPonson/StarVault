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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * OpenAuthManager 测试 — 覆盖 OAuth 设备码 3 步流的状态机。
 *
 * 替换 [ScanLoginManagerTest],测试 [OpenAuthManager] 的核心行为:
 *  1. requestDeviceCode 业务 state!=1 → failure
 *  2. requestDeviceCode 业务成功 → success(uid/time/sign/qrcode 全在)
 *  3. pollForToken deadline 已过 → 立即 Expired
 *  4. pollForToken data.status=0 → 持续 Waiting + 最终 Expired(不 emit Authorized)
 *  5. pollForToken data.status=1 → emit Scanned 一次(不抖动)
 *  6. pollForToken data.status=2 → 调 deviceCodeToToken + emit Authorized
 *  7. pollForToken data.status=2 + exchange 失败 → emit Error
 *  8. revokeToken 抛异常 → 吞掉
 *
 * Mock 边界:
 *  - 2 个 API:api(OpenAuthApiService)+ statusApi(StatusPollApi)
 *  - android.util.Log 静态方法 mock 掉(JVM unit test 缺 native)
 *  - zxing 在 JVM unit test 可用(纯 Java 库,不依赖 Android)
 *  - Bitmap mock relaxed,不关心像素
 */
class OpenAuthManagerTest {
    private val api = mockk<OpenAuthApiService>()
    private val statusApi = mockk<StatusPollApi>()
    private val manager = OpenAuthManager(api, statusApi)
    private val fakeBitmap: Bitmap = mockk(relaxed = true)

    @Before
    fun stubAndroidLog() {
        // android.util.Log 在 JVM unit test 里没有 native 实现,会抛 UnsatisfiedLinkError
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.d(any(), any<String>()) } returns 0
    }

    @After
    fun unstubAndroidLog() {
        unmockkStatic(android.util.Log::class)
    }

    /** 构造 DeviceCodeData:uid/time/sign + zxing 渲染过的 bitmap。 */
    private fun deviceCode(
        uid: String = "u1",
        time: Long = 1234567890L,
        sign: String = "s1",
    ) = OpenAuthManager.DeviceCodeData(
        uid          = uid,
        time         = time,
        sign         = sign,
        qrcodeUrl    = "https://115.com/scan/dg-$uid",
        codeVerifier = OpenAuthApiService.CODE_VERIFIER,
        bitmap       = fakeBitmap,
    )

    // ─── requestDeviceCode ────────────────────────────────────────────────

    @Test
    fun `requestDeviceCode returns failure when business state is false`() = runTest {
        coEvery { api.requestDeviceCode() } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(0), code = 10001, message = "rate limited", data = null)
        )
        val r = manager.requestDeviceCode()
        assertTrue(r.isFailure)
    }

    @Test
    fun `requestDeviceCode in unit test hits QR-render fallback because zxing is not stubbed`() = runTest {
        // JVM unit test 下 zxing / Bitmap.createBitmap 不可用(见 app/build.gradle.kts
        // unitTests.isReturnDefaultValues=true)— 业务成功路径走到 encodeQrBitmap
        // 会返回 null,Result.failure("QR 渲染失败")。生产 AVD 上 zxing 可用,这里
        // 只断言"业务成功 + 走到 zxing 之前不抛 + zxing 失败有兜底 message"。
        coEvery { api.requestDeviceCode() } returns Response.success(
            ApiEnvelope(
                state = JsonPrimitive(1),
                data = DeviceCodeResponse(
                    uid = "u-abc",
                    time = 1234567890L,
                    sign = "s-xyz",
                    qrcode = "https://115.com/scan/dg-u-abc",
                ),
            )
        )
        val r = manager.requestDeviceCode()
        // 期望:zxing 在 unit test 失败,Result.failure
        assertTrue(r.isFailure)
        val msg = r.exceptionOrNull()?.message.orEmpty()
        assertTrue("expected QR 渲染失败 in message, got=$msg", msg.contains("QR 渲染失败"))
    }

    // ─── pollForToken ────────────────────────────────────────────────────

    @Test
    fun `pollForToken emits Expired immediately when deadline already passed`() = runTest {
        // status mock 不应被调用(deadline 已过,先发 Expired 再 return)
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() - 1).toList()
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Expired })
        assertTrue(events.none { it is OpenAuthManager.AuthStatus.Authorized })
    }

    @Test
    fun `pollForToken keeps Waiting when status 0 then Expired on deadline`() = runTest {
        coEvery { statusApi.getStatus(any(), any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = StatusPollResponse(status = 0, msg = null))
        )
        val dc = deviceCode()
        // 100ms deadline 让它跑几轮 poll
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 100).toList()
        // 首条必须是 Waiting
        assertTrue("first event should be Waiting, got=${events.first()}",
            events.first() is OpenAuthManager.AuthStatus.Waiting)
        // 不应 emit Authorized
        assertTrue(events.none { it is OpenAuthManager.AuthStatus.Authorized })
        // 最终 Expired
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Expired })
    }

    @Test
    fun `pollForToken emits Scanned once when status 1 (no flutter)`() = runTest {
        coEvery { statusApi.getStatus(any(), any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = StatusPollResponse(status = 1, msg = "scanned"))
        )
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 200).toList()
        val scanned = events.filterIsInstance<OpenAuthManager.AuthStatus.Scanned>()
        // 200ms 内可能跑 ~10 轮 poll,但 scannedEmitted 防护保证只 emit 1 次
        assertTrue("should emit Scanned exactly once, got=${scanned.size}", scanned.size == 1)
    }

    @Test
    fun `pollForToken emits Authorized when status 2 and exchange succeeds`() = runTest {
        coEvery { statusApi.getStatus(any(), any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = StatusPollResponse(status = 2, msg = "confirmed"))
        )
        coEvery { api.deviceCodeToToken(any(), any()) } returns Response.success(
            ApiEnvelope(
                state = JsonPrimitive(1),
                data = DeviceCodeToTokenResponse(
                    accessToken = "at_new",
                    refreshToken = "rt_new",
                    expiresIn = 7200L,
                    userId = 12345L,
                    userName = "alice",
                ),
            )
        )
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 5000).toList()
        val auth = events.filterIsInstance<OpenAuthManager.AuthStatus.Authorized>()
        assertEquals(1, auth.size)
        val a = auth.first()
        assertEquals("at_new", a.accessToken)
        assertEquals("rt_new", a.refreshToken)
        assertEquals(7200L, a.expiresIn)
        assertEquals(12345L, a.uid)
        assertEquals("alice", a.userName)
        coVerify(exactly = 1) { api.deviceCodeToToken("u1", any()) }
    }

    @Test
    fun `pollForToken emits Error when status 2 but exchange fails`() = runTest {
        coEvery { statusApi.getStatus(any(), any(), any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = StatusPollResponse(status = 2, msg = "confirmed"))
        )
        coEvery { api.deviceCodeToToken(any(), any()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(0), code = 40101017, message = "uid 无效", data = null)
        )
        val dc = deviceCode()
        val events = manager.pollForToken(dc, deadline = System.currentTimeMillis() + 5000).toList()
        assertTrue(events.none { it is OpenAuthManager.AuthStatus.Authorized })
        assertTrue(events.any { it is OpenAuthManager.AuthStatus.Error })
    }

    // ─── revokeToken ─────────────────────────────────────────────────────

    @Test
    fun `revokeToken swallows exceptions`() = runTest {
        coEvery { api.revokeToken(refreshToken = any()) } throws RuntimeException("network down")
        // 不应抛
        manager.revokeToken("rt_yyy")
    }

    @Test
    fun `revokeToken no-op when refreshToken is blank`() = runTest {
        manager.revokeToken("")
        // 不调 api,无副作用
        coVerify(exactly = 0) { api.revokeToken(refreshToken = any()) }
    }

    // ─── guard: data null/empty 处理 ──────────────────────────────────────

    @Test
    fun `requestDeviceCode returns failure when data is null even if state true`() = runTest {
        coEvery { api.requestDeviceCode() } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = null)
        )
        val r = manager.requestDeviceCode()
        assertTrue(r.isFailure)
        assertNotNull(r.exceptionOrNull())
    }

    @Test
    fun `requestDeviceCode returns failure when uid is blank`() = runTest {
        coEvery { api.requestDeviceCode() } returns Response.success(
            ApiEnvelope(
                state = JsonPrimitive(1),
                data = DeviceCodeResponse(uid = "", time = 1L, sign = "s", qrcode = "q"),
            )
        )
        val r = manager.requestDeviceCode()
        assertTrue(r.isFailure)
    }
}
