package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.starvault.data.local.auth.Cloud115AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 115 扫码登录编排器。
 *
 *  调用流程（参考 Lumen 模式 + p115client 端点）：
 *    1. [getQRCode]    → GET /api/1.0/web/1.0/token/ 拿会话 → GET /api/1.0/web/1.0/qrcode?uid=... 下 PNG
 *    2. [signIn]       → 2s 间隔 GET /get/status/；
 *                        status=1 仅 emit Scanned（**不在此调 POST**，userInfo 拿不到）；
 *                        status=2 调 POST /app/1.0/qandroid/1.0/login/qrcode/，
 *                                  从响应 body `data.cookie` 拿 cookies（不是 Set-Cookie 头）。
 *
 *  状态机：
 *    Waiting → Scanned → Success(cookies + uid + userName + deviceName)
 *    任意态  → Cancelled / Timeout / Error(msg)
 *
 *  5 分钟 QR 过期由调用方 [LoginViewModel] 的 expire countdown 决定，本类不重复计时；
 *  仅在 signIn 入参 deadline 到期时 emit Timeout。
 *
 *  userInfo 注意点：
 *    115 端点上 /get/status/ 和 POST /app/1.0/.../login/qrcode/ 都不一定返回 user_name/device。
 *    status=1 时拿不到 userInfo 是预期（Lumen/p115client 都不在此时拉），
 *    status=2 时如果 data.user_name 为空，VM 端用"已登录"占位；uid 走 cookies 后的
 *    后续 webapi 调用（Phase 3 接入）补齐。
 */
class ScanLoginManager(
    private val api: ScanApiService,
    @Suppress("unused") private val authStore: Cloud115AuthStore,   // 预留：未来 SignOut 失效检测
) {

    companion object {
        private const val TAG = "ScanLoginManager"
        private const val POLLING_INTERVAL_MS = 2_000L
        private const val QR_CODE_TTL_MS = 5 * 60 * 1_000L
    }

    /** 独立 image client：不走 Retrofit Converter，直接拿 PNG bytes */
    private val imageClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

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

    /**
     * 一次性：拿 QR 会话 + 下载 QR PNG。
     * 失败时 Result.failure(IllegalStateException(msg))。
     */
    suspend fun getQRCode(): Result<QRCodeData> = withContext(Dispatchers.IO) {
        try {
            val tokenResp = api.getScanToken()
            if (!tokenResp.isSuccessful) return@withContext Result.failure(
                IllegalStateException("GET /qrcode HTTP ${tokenResp.code()}")
            )
            val token = tokenResp.body()?.takeIf { it.isOk }?.data
                ?: return@withContext Result.failure(IllegalStateException("GET /qrcode 业务失败"))

            val bitmap = downloadQrBitmap(token.uid)
                ?: return@withContext Result.failure(IllegalStateException("下载 QR 图片失败"))

            Log.i(TAG, "QR ready, uid=${token.uid.take(8)}...")
            Result.success(QRCodeData(token.uid, token.time, token.sign, bitmap))
        } catch (e: Throwable) {
            // catch Throwable 而非 Exception：BitmapFactory.decodeByteArray 在无 Android runtime 时
            // 抛 UnsatisfiedLinkError（Error 子类），也需要兜成 Result.failure
            Log.e(TAG, "getQRCode failed", e)
            Result.failure(e)
        }
    }

    private fun downloadQrBitmap(uid: String): Bitmap? {
        val url = "${Cloud115ApiClient.SCAN_BASE_URL}api/1.0/web/1.0/qrcode?uid=$uid"
        val req = Request.Builder().url(url).get().build()
        return imageClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bytes = resp.body?.bytes() ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    /**
     * 启动扫码流程。
     *
     * 调用方订阅 Flow：
     *  - [ScanStatus.Scanned]  → UI 高亮「已扫码」+ 等待用户点确认
     *  - [ScanStatus.Success]  → 拿到 cookies + userInfo，调用方落 DataStore + 跳 Home
     *  - [ScanStatus.Cancelled] / [ScanStatus.Timeout] / [ScanStatus.Error] → UI 提示并可重试
     */
    fun signIn(
        qr: QRCodeData,
        deadline: Long = System.currentTimeMillis() + QR_CODE_TTL_MS,
    ): Flow<ScanStatus> = flow {
        // 起始：先发一条 Waiting（带 bitmap）让 UI 立刻渲染
        emit(ScanStatus.Waiting(qr.bitmap))

        while (true) {
            if (System.currentTimeMillis() >= deadline) {
                emit(ScanStatus.Timeout)
                return@flow
            }
            try {
                val resp = api.checkScanStatus(qr.uid, qr.time, qr.sign)
                if (!resp.isSuccessful) {
                    emit(ScanStatus.Error("HTTP ${resp.code()}"))
                    delay(POLLING_INTERVAL_MS); continue
                }
                val data = resp.body()?.takeIf { it.isOk }?.data
                if (data == null) {
                    emit(ScanStatus.Error(resp.body()?.message ?: "查询失败"))
                    delay(POLLING_INTERVAL_MS); continue
                }
                when (data.status) {
                    0 -> { /* 仍等待，不发新事件 */ }
                    1 -> {
                        // Scanned：115 在此状态不返回 userInfo，**不要**调 POST（那是确认登录的端点）。
                        // Lumen/p115client 都只是单纯 emit Scanned 等用户点确认。
                        // nickname/device 用默认值（VM 端会用「已扫码」/「请在手机确认」占位）。
                        emit(ScanStatus.Scanned(nickname = "", deviceName = ""))
                    }
                    2 -> {
                        // Confirmed：调 POST /app/1.0/{app}/1.0/login/qrcode/，从 body `data.cookie` 拿 cookies
                        val result = fetchLoginResult(qr.uid)
                        if (result != null) {
                            val (userInfo, cookies) = result
                            emit(
                                ScanStatus.Success(
                                    cookies = cookies,
                                    uid = userInfo?.userId ?: 0L,
                                    userName = userInfo?.userName.orEmpty(),
                                    deviceName = userInfo?.device.orEmpty(),
                                )
                            )
                            return@flow
                        } else {
                            emit(ScanStatus.Error("cookies 提取失败"))
                            return@flow
                        }
                    }
                    -1 -> {
                        emit(ScanStatus.Cancelled)
                        return@flow
                    }
                    else -> emit(ScanStatus.Error("未知状态 ${data.status}"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "polling exception", e)
                emit(ScanStatus.Error(e.message ?: "轮询异常"))
            }
            delay(POLLING_INTERVAL_MS)
        }
    }

    /**
     * 调 POST /app/1.0/{app}/1.0/login/qrcode/ 拿 cookies（**仅在 status=2 时调**）。
     *
     *  cookies 来自响应 body 的 `data.cookie` 字段（**不是 Set-Cookie 头**）。
     *  115 实际返回的 shape（实测）：
     *    - JSON 对象 `{"UID":"xxx","CID":"xxx","SEID":"xxx","KID":"xxx"}`
     *      → 我们用 `Map<String, String>` 接，再拼成 "UID=xxx; CID=xxx; ..."
     *
     *  userInfo（userId/userName/device）115 不一定回，nullable。
     */
    private suspend fun fetchLoginResult(uid: String): Pair<LoginResultData?, String>? {
        val resp = api.getLoginResult(app = "qandroid", appName = "qandroid", account = uid)
        if (!resp.isSuccessful) {
            Log.w(TAG, "getLoginResult HTTP ${resp.code()}")
            return null
        }
        val data = resp.body()?.takeIf { it.isOk }?.data
            ?: return null
        val cookieMap = data.cookie?.takeIf { it.isNotEmpty() } ?: return null
        val cookies = cookieMap.entries.joinToString("; ") { (k, v) -> "$k=$v" }
        return data to cookies
    }
}
