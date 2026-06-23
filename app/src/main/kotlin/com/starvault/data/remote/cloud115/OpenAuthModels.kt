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
 *  - time      : 服务端时间（秒）
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
 * GET /open/authDeviceCode（轮询）的 data 字段。
 *
 * 字段映射：
 *  - accessToken / refreshToken : 必须存在才能落库，缺失视为"用户尚未确认"继续轮询
 *  - expiresIn    : 秒，相对值；存库时转 expiresAtMs = nowMs + expiresIn*1000
 *  - scope / tokenType : 调试用
 *  - userId / userName : 115 用户信息
 *
 * 任意必填字段缺失时由调用方决定继续轮询还是终止流。
 */
@Serializable
data class TokenPollResponse(
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