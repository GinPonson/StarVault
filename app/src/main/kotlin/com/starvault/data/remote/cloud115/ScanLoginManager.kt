package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import com.starvault.data.local.auth.Cloud115AuthStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 115 扫码登录编排器（占位版，Task 6 替换为完整实现）。
 */
class ScanLoginManager(
    private val api: ScanApiService,
    private val authStore: Cloud115AuthStore,
) {
    data class QRCodeData(val uid: String, val time: Long, val sign: String, val bitmap: Bitmap)

    sealed class ScanStatus {
        data class Waiting(val bitmap: Bitmap) : ScanStatus()
        data class Scanned(val nickname: String, val deviceName: String) : ScanStatus()
        data class Success(
            val cookies: String,
            val uid: Long,
            val userName: String,
            val deviceName: String,
        ) : ScanStatus()
        data object Cancelled : ScanStatus()
        data object Timeout : ScanStatus()
        data class Error(val message: String) : ScanStatus()
    }

    suspend fun getQRCode(): Result<QRCodeData> = Result.failure(NotImplementedError())
    fun signIn(qr: QRCodeData, deadline: Long = System.currentTimeMillis() + 5 * 60 * 1_000L): Flow<ScanStatus> = emptyFlow()
}
