package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import com.starvault.data.local.auth.Cloud115AuthStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
            ApiEnvelope(state = 0, code = 10001, message = "rate limited", data = null)
        )
        val r = manager.getQRCode()
        assertTrue(r.isFailure)
    }

    @Test
    fun `signIn emits Cancelled when status=-1`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = 1, data = QrStatusData(status = -1))
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 5_000).toList()
        assertTrue(events.any { it is ScanLoginManager.ScanStatus.Cancelled })
    }

    @Test
    fun `signIn emits Timeout when deadline passed`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = 1, data = QrStatusData(status = 0))
        )
        // 设 deadline=now → 立即超时
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() - 1).toList()
        assertTrue(events.any { it is ScanLoginManager.ScanStatus.Timeout })
    }

    @Test
    fun `signIn emits Scanned when status=1 and userInfo fetched`() = runTest {
        coEvery { api.checkScanStatus(any<String>(), any<Long>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = 1, data = QrStatusData(status = 1))
        )
        coEvery { api.getLoginResult(any<String>(), any<String>(), any<String>()) } returns Response.success(
            ApiEnvelope(state = 1, data = LoginResultData(userId = 1L, userName = "alice", device = "iPhone"))
        )
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        // 短 deadline 强制让 flow 在几次 poll 后超时退出（status=1 不会自己终止 loop）
        val events = manager.signIn(qr, deadline = System.currentTimeMillis() + 100).toList()
        val scanned = events.filterIsInstance<ScanLoginManager.ScanStatus.Scanned>().firstOrNull()
        assertEquals("alice", scanned?.nickname)
        assertEquals("iPhone", scanned?.deviceName)
    }
}
