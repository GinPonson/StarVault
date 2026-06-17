package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 115 API 统一响应壳：{state, code, message, data}
 *  - state=1  → 业务成功（data 非空）
 *  - state=0  → 业务失败（message 有内容）
 *  - HTTP 层失败（404/500）由 Retrofit Response.isSuccessful 判定
 *
 *  注：state 实际是 Int（0/1）而非 Boolean，需要在调用方 takeIf { it.state == 1 } 判定。
 */
@Serializable
data class ApiEnvelope<T>(
    val state: Int = 0,
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null,
)

/** GET /api/1.0/web/1.0/qrcode/ 响应 data：扫码会话建立 */
@Serializable
data class QrTokenData(
    val uid: String,
    val time: Long = 0L,
    val sign: String = "",
    @SerialName("qrcode") val qrcodeUrl: String = "",
)

/** GET /get/status/ 响应 data */
@Serializable
data class QrStatusData(
    val status: Int = 0,           // 0=Waiting, 1=Scanned, 2=Confirmed, -1=Cancelled
    val version: String = "",      // 服务端版本 hash，扫码轮询里不带 sign/time
    val msg: String = "",
)

/** GET /app/1.0/qandroid/1.0/login/qrcode/ 响应 data（扫码确认后回包，含扫码用户信息）*/
@Serializable
data class LoginResultData(
    @SerialName("user_id") val userId: Long = 0L,
    @SerialName("user_name") val userName: String = "",
    val device: String = "",
)
