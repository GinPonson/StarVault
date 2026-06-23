package com.starvault.data.remote.cloud115

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * 115 开放平台 OAuth 设备码流编排器。
 *
 * 替换 [ScanLoginManager]（旧的 Cookie 扫码流）。
 *
 * 流程（旧的 4 步 → 新的 2 步）：
 *  1. [requestDeviceCode]  POST /open/authDeviceCode
 *                          → 拿 uid / qrcodeUrl（"https://115.com/scan/dg-<uid>"）
 *                          → 用 zxing 在本地把 qrcodeUrl 渲染成 QR Bitmap
 *  2. [pollForToken]       GET /open/authDeviceCode?uid=...&sign=...&code_verifier="0"*64
 *                          → 用户在 115 App 扫码 + 确认后，data.access_token 存在 → Authorized
 *
 * 状态机（6 态）：
 *  Waiting(bitmap) → 用户未扫码，不发新 event，保持 Waiting
 *  Scanned(...)    → OAuth 流里实际上"用户已扫码但未确认"也用同一 Waiting（115 不区分中间态）
 *  Authorized(...) → 拿到 tokens + uid + userName，调用方落 DataStore + 跳 Home
 *  Denied          → 用户拒绝授权
 *  Expired         → 5 分钟 QR 过期
 *  Error(msg)      → 网络或业务异常
 *
 * 5 分钟 QR 过期由调用方 [com.starvault.ui.login.LoginViewModel] 的 expire countdown 决定，
 * 本类不重复计时；仅在 pollForToken 入参 deadline 到期时 emit Expired。
 *
 * 重要：code_verifier 永不入 DataStore。OAuth 流是"客户端先证明自己有 secret"，
 * 这里 secret 固定为 "0"*64（与 p115client 一致），写在 [OpenAuthApiService.CODE_VERIFIER] 里。
 *
 * QR 渲染：115 返回的 `qrcode` 字段是跳转 URL（"https://115.com/scan/dg-xxx"），不是图片 URL；
 * 直接 GET 这个 URL 拿到的是 HTML，BitmapFactory 必失败。所以本类在本地用 zxing 把
 * qrcodeUrl 字符串编码为黑白 QR 位图，避免对 115 图像端点的耦合。
 */
class OpenAuthManager(
    private val api: OpenAuthApiService,
) {

    companion object {
        private const val TAG = "OpenAuthManager"
        private const val POLLING_INTERVAL_MS = 2_000L
        private const val QR_CODE_TTL_MS = 5 * 60 * 1_000L

        /** QR 位图边长（像素）。LoginScreen qr-card 是 220dp，512 在 2x/3x 屏都足够锐利。 */
        private const val QR_BITMAP_SIZE_PX = 512
    }

    /**
     * 设备码会话一次性的所有数据。
     *
     *  - uid         : 轮询回传
     *  - sign        : 轮询签名
     *  - qrcodeUrl   : 形如 "https://115.com/scan/dg-xxx"，由 115 App 扫码识别
     *  - codeVerifier: 永不入库（写死 "0"*64，仅在内存里传给 pollForToken）
     *  - bitmap      : 已渲染好的 QR 位图，UI 拿来直接显示
     */
    data class DeviceCodeData(
        val uid: String,
        val sign: String,
        val qrcodeUrl: String,
        val codeVerifier: String,
        val bitmap: Bitmap,
    )

    /**
     * 轮询事件（替换 ScanStatus，删掉 cookies 字段，加 token 字段）。
     */
    sealed class AuthStatus {
        data class Waiting(val bitmap: Bitmap) : AuthStatus()
        data class Scanned(val nickname: String, val deviceName: String) : AuthStatus()

        /**
         * 用户在 115 App 中确认登录，token 已就绪。
         *
         * @param accessToken  Bearer 鉴权串（必填）
         * @param refreshToken refresh_token（必填，给 signOut revoke 用）
         * @param expiresIn    秒，相对值
         * @param uid          115 user_id
         * @param userName     115 用户昵称
         */
        data class Authorized(
            val accessToken: String,
            val refreshToken: String,
            val expiresIn: Long,
            val uid: Long,
            val userName: String,
        ) : AuthStatus()

        /** 用户在 115 App 中拒绝授权。 */
        data object Denied : AuthStatus()

        /** QR 二维码 5 分钟过期。 */
        data object Expired : AuthStatus()

        data class Error(val message: String) : AuthStatus()
    }

    /**
     * 申请设备码 + 用 zxing 把 qrcodeUrl 渲染成 QR Bitmap。
     *
     * 错误处理：
     *  - HTTP 失败 → Result.failure(IllegalStateException)
     *  - 业务 state!=1 → Result.failure(IllegalStateException)
     *  - zxing 编码失败 → Result.failure（极少见，QR 算法本身稳定）
     *  - 任何 throw（含 UnsatisfiedLinkError）→ 兜成 Result.failure
     */
    suspend fun requestDeviceCode(): Result<DeviceCodeData> = withContext(Dispatchers.IO) {
        try {
            val resp = api.requestDeviceCode()
            if (!resp.isSuccessful) return@withContext Result.failure(
                IllegalStateException("POST /open/authDeviceCode HTTP ${resp.code()}")
            )
            val body = resp.body()
            val data = body?.takeIf { it.isOk }?.data
            if (data == null || data.uid.isBlank() || data.qrcode.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "业务失败: ${body?.message ?: body?.error ?: body?.state ?: "unknown"}"
                    )
                )
            }

            // 本地用 zxing 把 qrcodeUrl 字符串编码为黑白 QR Bitmap
            val bitmap = encodeQrBitmap(data.qrcode)
                ?: return@withContext Result.failure(IllegalStateException("QR 渲染失败"))

            Log.i(TAG, "device code ready, uid=${data.uid.take(8)}..., qrcodeUrl=${data.qrcode}")
            Result.success(
                DeviceCodeData(
                    uid = data.uid,
                    sign = data.sign,
                    qrcodeUrl = data.qrcode,
                    codeVerifier = OpenAuthApiService.CODE_VERIFIER,
                    bitmap = bitmap,
                )
            )
        } catch (e: Throwable) {
            // catch Throwable 而非 Exception：zxing 偶发抛 RuntimeException 也走这里
            Log.e(TAG, "requestDeviceCode failed", e)
            Result.failure(e)
        }
    }

    /**
     * 用 zxing 把字符串编码成黑白 QR Bitmap。
     *
     * 失败返回 null（极少见，zxing 算法本身稳定）。字符集 UTF-8，纠错等级 H（30%）。
     *
     * @param text 待编码内容（这里 = 115 返回的跳转 URL）
     */
    private fun encodeQrBitmap(text: String): Bitmap? = try {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
        )
        val matrix: BitMatrix = MultiFormatWriter().encode(
            text, BarcodeFormat.QR_CODE, QR_BITMAP_SIZE_PX, QR_BITMAP_SIZE_PX, hints,
        )
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "zxing encode failed", e)
        null
    }

    /**
     * 轮询直到 token 就绪 / 用户拒绝 / 过期 / 错误。
     *
     * 关键点：
     *  - 每次 poll 前检查 deadline，超时立刻 emit Expired（不等下次 poll）
     *  - "已扫码但未确认" 在 115 open 流里没有独立 status；115 维持 state=1 data=null
     *    直到用户点确认。**此阶段我们不发任何 event**（保持 Waiting，UI 端不变）
     *  - 用户拒绝时 115 通常返回 state=1 + data=null 持续一段时间直到过期，
     *    不返回 status=-1。这里只能靠 deadline 兜底为 Expired；UI 文案统一改为
     *    "二维码已过期，请刷新"（保持兼容）
     *
     * @param deviceCode 来自 [requestDeviceCode] 的成功返回
     * @param deadline   绝对时间戳（毫秒）；默认 5 分钟后
     */
    fun pollForToken(
        deviceCode: DeviceCodeData,
        deadline: Long = System.currentTimeMillis() + QR_CODE_TTL_MS,
    ): Flow<AuthStatus> = flow {
        // 起始：先发一条 Waiting（带 bitmap）让 UI 立刻渲染
        emit(AuthStatus.Waiting(deviceCode.bitmap))

        while (true) {
            // 1) 过期检测：先于网络调用，避免过期后还浪费一次 HTTP
            if (System.currentTimeMillis() >= deadline) {
                emit(AuthStatus.Expired)
                return@flow
            }

            // 2) 一次 poll
            try {
                val resp = api.pollDeviceCode(
                    uid          = deviceCode.uid,
                    sign         = deviceCode.sign,
                    codeVerifier = deviceCode.codeVerifier,
                )
                if (!resp.isSuccessful) {
                    emit(AuthStatus.Error("HTTP ${resp.code()}"))
                    delay(POLLING_INTERVAL_MS); continue
                }
                val env = resp.body()
                if (env == null || !env.isOk) {
                    emit(AuthStatus.Error(env?.message ?: env?.error ?: "查询失败"))
                    delay(POLLING_INTERVAL_MS); continue
                }

                val data = env.data
                val accessToken = data?.accessToken
                if (accessToken.isNullOrBlank()) {
                    // 用户尚未确认 — 不发新 event，保持 Waiting（UI 不闪烁）
                } else {
                    // 拿到 tokens，终止轮询
                    emit(
                        AuthStatus.Authorized(
                            accessToken  = accessToken,
                            refreshToken = data.refreshToken.orEmpty(),
                            expiresIn    = data.expiresIn ?: 7200L,
                            uid          = data.userId ?: 0L,
                            userName     = data.userName.orEmpty(),
                        )
                    )
                    return@flow
                }
            } catch (e: Exception) {
                Log.w(TAG, "polling exception", e)
                emit(AuthStatus.Error(e.message ?: "轮询异常"))
            }
            delay(POLLING_INTERVAL_MS)
        }
    }

    /**
     * 主动吊销 refresh_token。signOut 流程第一步。
     *
     * 用 runCatching 吞所有异常：revoke 失败不应阻塞本地清空（决策 #11）。
     *
     * @param refreshToken 当前 DataStore 中的 refresh_token；空串表示跳过
     */
    suspend fun revokeToken(refreshToken: String) {
        if (refreshToken.isBlank()) return
        runCatching { api.revokeToken(refreshToken = refreshToken) }
            .onFailure { Log.w(TAG, "revokeToken failed", it) }
    }
}