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

/** GET /get/status/ 响应 data。
 *
 *  115 实际返回的字段（实测 + Lumen 推测）：
 *    - status   : Int  0=等待扫码, 1=已扫码待确认, 2=已确认, -1=已取消
 *    - version  : String  服务端版本 hash（轮询不带 sign/time 但服务端会回）
 *    - msg      : String  状态描述文本
 *    - uid      : String?  扫码用户 ID（status=2 时返回）
 *    - userName : String?  扫码用户昵称（status=2 时返回）
 *
 *  注：status=1 时本字段通常只含 status/version/msg；userInfo 不在轮询里，
 *      需要在 status=2 后调 POST /app/1.0/{app}/1.0/login/qrcode/ 拿。
 */
@Serializable
data class QrStatusData(
    val status: Int = 0,
    val version: String = "",
    val msg: String = "",
    val uid: String? = null,
    @SerialName("user_name") val userName: String? = null,
)

/** POST /app/1.0/{app}/1.0/login/qrcode/ 响应 data（用户点确认后回包）。
 *
 *  关键字段：
 *    - cookie : Map<String, String>?  115 登录 cookies（**这是核心**，直接用作后续请求的 Cookie 头）
 *      实际是 JSON 对象 `{"UID":"...","CID":"...","SEID":"...","KID":"..."}`；
 *      用 Map 接，让 [com.starvault.data.remote.cloud115.ScanLoginManager.fetchLoginResult]
 *      拼接成 "UID=...; CID=...; SEID=...; KID=..."。
 *
 *  可选字段（115 不一定回，全部 nullable + 默认 null）：
 *    - userId / userName / device : 扫码用户信息
 */
@Serializable
data class LoginResultData(
    val cookie: Map<String, String>? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("user_name") val userName: String? = null,
    val device: String? = null,
)
