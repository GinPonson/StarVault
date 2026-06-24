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
import java.net.SocketTimeoutException

/**
 * 115 开放平台 OAuth 设备码流编排器。
 *
 * 三步流程：
 *  1. [requestDeviceCode] POST /open/authDeviceCode
 *                          → 拿 uid / time / qrcodeUrl（"https://115.com/scan/dg-<uid>"）
 *                          → 用 zxing 在本地把 qrcodeUrl 渲染成 QR Bitmap
 *  2. [pollForToken]       GET /get/status/  (long-poll)
 *                          → status=0 继续等；status=1 emit Scanned 一次；
 *                          → status=2 调 deviceCodeToToken 换真 token → emit Authorized
 *  3. (内部) exchangeForToken POST /open/deviceCodeToToken
 *
 * 状态机（6 态）：
 *  Waiting(bitmap) → status=0，不发新 event（UI 不闪烁）
 *  Scanned(...)    → status=1，emit 一次（提示用户去 115 App 点确认）
 *  Authorized(...) → 拿到 tokens + uid + userName，调用方落 DataStore + 跳 Home
 *  Denied          → 用户拒绝授权
 *  Expired         → 5 分钟 QR 过期
 *  Error(msg)      → 网络或业务异常
 *
 * 5 分钟 QR 过期由调用方 [com.starvault.ui.login.LoginViewModel] 的 expire countdown 决定，
 * 本类不重复计时；仅在 pollForToken 入参 deadline 到期时 emit Expired。
 *
 * 重要:code_verifier 永不入 DataStore。OAuth 流是"客户端先证明自己有 secret",
 * 这里 secret 固定为 "0"*64(对齐 OpenList 115-sdk-go),写在 [OpenAuthApiService.CODE_VERIFIER] 里。
 * code_challenge = base64(sha256(verifier))(由 115 服务端验签)。
 *
 * QR 渲染：115 返回的 `qrcode` 字段是跳转 URL（"https://115.com/scan/dg-xxx"），不是图片 URL；
 * 直接 GET 这个 URL 拿到的是 HTML，BitmapFactory 必失败。所以本类在本地用 zxing 把
 * qrcodeUrl 字符串编码为黑白 QR 位图，避免对 115 图像端点的耦合。
 */
class OpenAuthManager(
    private val api: OpenAuthApiService,
    private val statusApi: StatusPollApi,
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
     *  - time        : 服务端时间（秒），轮询时也要回传给 status 端点
     *  - sign        : 轮询签名
     *  - qrcodeUrl   : 形如 "https://115.com/scan/dg-xxx"，由 115 App 扫码识别
     *  - codeVerifier: 永不入库（写死 "0"*64，仅在内存里传给 deviceCodeToToken）
     *  - bitmap      : 已渲染好的 QR 位图，UI 拿来直接显示
     */
    data class DeviceCodeData(
        val uid: String,
        val time: Long = 0L,
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

            Log.i(TAG, "device code ready, uid=${data.uid.take(8)}..., time=${data.time}, qrcodeUrl=${data.qrcode}")
            Result.success(
                DeviceCodeData(
                    uid = data.uid,
                    time = data.time,
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
     * 轮询 status 直到 token 就绪 / 用户拒绝 / 过期 / 错误。
     *
     * 三步流程：
     *  1. GET /get/status/ 长轮询
     *     - status=0（未扫/未确认）→ 不发新 event，保持 Waiting
     *     - status=1（已扫未确认） → emit Scanned 一次（避免抖动）
     *     - status=2（已确认）     → 调 deviceCodeToToken 换 token
     *  2. POST /open/deviceCodeToToken（仅 status=2 时）
     *  3. emit Authorized(accessToken, refreshToken, ...)
     *
     * 关键点：
     *  - 每次 poll 前检查 deadline，超时立刻 emit Expired（不等下次 poll）
     *  - SocketTimeoutException 特判为"长轮询正常超时" → 不 emit Error，继续 loop
     *  - 其它 Exception → emit Error 后继续 loop（不退出，给用户重试机会）
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

        var scannedEmitted = false  // status==1 只 emit 一次，避免抖动
        while (true) {
            // 1) 过期检测：先于网络调用，避免过期后还浪费一次 HTTP
            if (System.currentTimeMillis() >= deadline) {
                emit(AuthStatus.Expired)
                return@flow
            }

            // 2) 一次 long-poll
            try {
                val resp = statusApi.getStatus(
                    uid         = deviceCode.uid,
                    time        = deviceCode.time,
                    sign        = deviceCode.sign,
                    cacheBuster = System.currentTimeMillis(),
                )
                val env = resp.body()
                if (env == null || !env.isOk) {
                    emit(AuthStatus.Error(env?.message ?: env?.error ?: "状态查询失败"))
                    delay(POLLING_INTERVAL_MS); continue
                }

                when (env.data?.status ?: 0) {
                    0 -> { /* 未扫 / 未确认 — 不发新 event，保持 Waiting（UI 不闪烁） */ }
                    1 -> if (!scannedEmitted) {
                        emit(AuthStatus.Scanned("", "请在 115 App 中点击「确认登录」"))
                        scannedEmitted = true
                    }
                    2 -> {
                        // 已确认 → 换真 token，终止轮询
                        val tok = exchangeForToken(deviceCode)
                        return@flow tok.fold(
                            onSuccess = { data ->
                                emit(
                                    AuthStatus.Authorized(
                                        accessToken  = data.accessToken.orEmpty(),
                                        refreshToken = data.refreshToken.orEmpty(),
                                        expiresIn    = data.expiresIn ?: 7200L,
                                        uid          = data.userId ?: 0L,
                                        userName     = data.userName.orEmpty(),
                                    )
                                )
                            },
                            onFailure = { e ->
                                Log.e(TAG, "deviceCodeToToken failed", e)
                                emit(AuthStatus.Error(e.message ?: "换 token 失败"))
                            },
                        )
                    }
                    else -> {
                        emit(AuthStatus.Error("未知 status=${env.data?.status}"))
                        delay(POLLING_INTERVAL_MS); continue
                    }
                }
            } catch (e: SocketTimeoutException) {
                // 长轮询正常超时（65s read timeout 已到，但服务端没回），按"还在等"处理
                Log.v(TAG, "status long-poll timeout, keep waiting")
            } catch (e: Exception) {
                Log.w(TAG, "polling exception", e)
                emit(AuthStatus.Error(e.message ?: "轮询异常"))
            }
            delay(POLLING_INTERVAL_MS)
        }
    }

    /**
     * 用设备码换真 access_token（status==2 后调一次）。
     *
     * @return Result.success(DeviceCodeToTokenResponse) / Result.failure(异常)
     */
    private suspend fun exchangeForToken(
        dc: DeviceCodeData,
    ): Result<DeviceCodeToTokenResponse> = try {
        val resp = api.deviceCodeToToken(uid = dc.uid, codeVerifier = dc.codeVerifier)
        if (!resp.isSuccessful) {
            Result.failure(IllegalStateException("POST /open/deviceCodeToToken HTTP ${resp.code()}"))
        } else {
            val data = resp.body()?.takeIf { it.isOk }?.data
            if (data == null || data.accessToken.isNullOrBlank()) {
                Result.failure(
                    IllegalStateException("换 token 业务失败: ${resp.body()?.message ?: "unknown"}")
                )
            } else Result.success(data)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "deviceCodeToToken threw", e)
        Result.failure(e)
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