package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 115 开放平台 OAuth 设备码流的 DTO 集合。
 *
 * 替换 [ScanModels] 中扫码专用的 `QrTokenData` / `QrStatusData` / `LoginResultData`。
 * 端点契约见 plan §2。
 *
 * 设计要点：
 *  - 所有字段尽量 nullable + 默认 null，避免 115 偶尔缺字段时反序列化失败
 *  - snake_case → camelCase 走 `@SerialName`
 *  - 与 `ScanModels.ApiEnvelope<T>` 复用同一外层壳（state=1 即成功）
 */

/**
 * POST /open/authDeviceCode 的 data 字段。
 *
 * 字段映射：
 *  - uid       : 设备码会话 ID，轮询时回传
 *  - time      : 服务端时间（秒），[StatusPollApi] 轮询时也要回传
 *  - sign      : 轮询签名
 *  - qrcode    : 形如 "https://115.com/scan/dg-<uid>" 的跳转链接（不是图片 URL）
 *  - expiresIn : 设备码 TTL（秒），可选，115 不一定回
 */
@Serializable
data class DeviceCodeResponse(
    val uid: String = "",
    val time: Long = 0L,
    val sign: String = "",
    val qrcode: String = "",
    @SerialName("expires_in") val expiresIn: Long? = null,
)

/**
 * GET /get/status/ 长轮询的 data 字段。
 *
 * 字段映射：
 *  - status : 0=未扫/未确认（空 data 也按 0 处理），1=已扫未确认，2=已确认 → 触发 deviceCodeToToken
 *  - msg    : 服务端附带消息，可选
 *
 * 注：115 在用户未扫码时可能返回 `data: {}`（空对象），kotlinx.serialization 会用默认值填充，
 * 此时 `status = 0`，调用方按 "继续等" 处理（不发新 event）。
 */
@Serializable
data class StatusPollResponse(
    val status: Int = 0,
    val msg: String? = null,
)

/**
 * POST /open/deviceCodeToToken 的 data 字段（真 token 来源）。
 *
 * 字段映射：
 *  - accessToken / refreshToken : 必须存在才能落库，缺失视为 "换 token 业务失败"
 *  - expiresIn    : 秒，相对值；存库时转 expiresAtMs = nowMs + expiresIn*1000
 *  - scope / tokenType : 调试用
 *  - userId / userName : 115 用户信息
 *
 * 三步流专用：status==2 后调 deviceCodeToToken 拿真 token 时使用。
 */
@Serializable
data class DeviceCodeToTokenResponse(
    @SerialName("access_token")  val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in")    val expiresIn: Long? = null,
    val scope: String? = null,
    @SerialName("token_type")    val tokenType: String? = null,
    @SerialName("user_id")       val userId: Long? = null,
    @SerialName("user_name")     val userName: String? = null,
)

/**
 * POST /open/authTokenRefresh 的 data。
 *
 * 当前不在本期接入 refresh，留接口位便于 Phase 2 接入自动续期。
 */
@Serializable
data class TokenRefreshResponse(
    @SerialName("access_token")  val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in")    val expiresIn: Long? = null,
    @SerialName("token_type")    val tokenType: String? = null,
)

/**
 * POST /open/authTokenRevoke 的 data。
 *
 * signOut 用；返回形状不固定，全 nullable 容错。调用方无需读 body。
 */
@Serializable
data class TokenRevokeResponse(
    val state: Int? = null,
    val message: String? = null,
)