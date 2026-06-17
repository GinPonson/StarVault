package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import com.starvault.data.local.auth.Cloud115AuthStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ScanLoginManagerTest {
    private val api = mockk<ScanApiService>()
    private val store = mockk<Cloud115AuthStore>(relaxed = true)
    private val manager = ScanLoginManager(api, store)
    private val fakeBitmap: Bitmap = mockk(relaxed = true)

    @Test
    fun `getQRCode returns failure when token business-state is false`() = runTest {
        // 走 "state=false 业务失败" 快速路径，避免触发 Log.e（android.util.Log 静态初始化需要 Android runtime）
        coEvery { api.getScanToken() } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(0), code = 10001, message = "rate limited", data = null)
        )
        val r = manager.getQRCode()
        assertTrue(r.isFailure)
    }

    @Test
    fun `signIn emits Cancelled when status=-1`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = -1))
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 5_000).toList()
        assertTrue(events.any { it is ScanLoginManager.ScanStatus.Cancelled })
    }

    @Test
    fun `signIn emits Timeout when deadline passed`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = 0))
        )
        // 设 deadline=now → 立即超时
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() - 1).toList()
        assertTrue(events.any { it is ScanLoginManager.ScanStatus.Timeout })
    }

    @Test
    fun `signIn emits Scanned without calling getLoginResult when status=1`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = 1))
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        // 短 deadline 强制让 flow 在几次 poll 后超时退出（status=1 不会自己终止 loop）
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 100).toList()
        val scanned = events.filterIsInstance<ScanLoginManager.ScanStatus.Scanned>().firstOrNull()
        assertTrue("expected Scanned event", scanned != null)
        // 关键：status=1 不应该触发 POST（POST 是 status=2 拿 cookies 用的；Lumen/p115client 都不在此调）
        coVerify(exactly = 0) { api.getLoginResult(any<String>(), any<String>(), any<String>()) }
    }

    @Test
    fun `signIn emits Success with cookies from data-cookie when status=2`() = runTest {
        // /get/status/ 第一次回 status=1，第二次回 status=2 → 触发 POST 拿 cookies
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returnsMany listOf(
            Response.success(ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = 1))),
            Response.success(ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = 2))),
        )
        coEvery { api.getLoginResult("qandroid", "qandroid", "u1") } returns Response.success(
            ApiEnvelope(
                state = JsonPrimitive(1),
                data = LoginResultData(
                    cookie = mapOf(
                        "UID" to "16757789_M1_1781700682",
                        "CID" to "abc123",
                        "SEID" to "def456",
                        "KID" to "ghi789",
                    ),
                    userId = 12345L,
                    userName = "alice",
                    device = "iPhone",
                ),
            )
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 5_000).toList()
        val success = events.filterIsInstance<ScanLoginManager.ScanStatus.Success>().firstOrNull()
        assertTrue("expected Success event", success != null)
        // 拼接顺序按 Map.entries 自然顺序（mockk 的 LinkedHashMap 保留插入顺序）
        assertEquals(
            "UID=16757789_M1_1781700682; CID=abc123; SEID=def456; KID=ghi789",
            success?.cookies,
        )
        assertEquals(12345L, success?.uid)
        assertEquals("alice", success?.userName)
        assertEquals("iPhone", success?.deviceName)
    }

    @Test
    fun `signIn emits Error when status=2 but cookie is missing`() = runTest {
        // status=2 时 115 返回但 cookies 字段空（极少见，IP 异常等场景）→ 应当 Error 不 Success
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returnsMany listOf(
            Response.success(ApiEnvelope(state = JsonPrimitive(1), data = QrStatusData(status = 2))),
        )
        coEvery { api.getLoginResult("qandroid", "qandroid", "u1") } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = LoginResultData(cookie = null))
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 5_000).toList()
        assertTrue(events.none { it is ScanLoginManager.ScanStatus.Success })
        assertTrue(events.any { it is ScanLoginManager.ScanStatus.Error })
    }
}
