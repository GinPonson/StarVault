package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 115 API 统一响应壳（同时兼容两套 115 域的 state 约定）。
 *
 *  - scan 域（qrcodeapi.115.com）：`state: 0/1` (Int)
 *  - webapi 域（webapi.115.com）  ：`state: true/false` (Boolean)
 *
 *  字段差异（webapi 用 `error` + `errno` 表示失败，scan 用 `code` + `message`）：
 *  - message : scan 失败文本
 *  - error   : webapi 失败文本
 *  - code    : scan 错误码
 *  - errno   : webapi 错误码
 *
 *  HTTP 层失败（404/500）由 Retrofit Response.isSuccessful 判定。
 *  业务成功判定：用 [isOk] 扩展属性，同时接受 `state=1` 与 `state=true`。
 */
@Serializable
data class ApiEnvelope<T>(
    val state: JsonElement? = null,
    val code: Int = 0,
    val message: String? = null,
    val error: String? = null,
    @SerialName("errno") val errno: Int? = null,
    val data: T? = null,
)

/**
 * 业务成功判定：state=1 (Int) 或 state=true (Boolean)。
 * 缺失 / null / 0 / false / 其他值都视为失败。
 *
 *  使用：[com.starvault.data.remote.cloud115.ScanLoginManager] 扫码域、
 *         [com.starvault.data.repository.AuthRepository] webapi 域都通过此判定。
 */
val ApiEnvelope<*>.isOk: Boolean
    get() {
        val s = state as? JsonPrimitive ?: return false
        return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
    }

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

/* ─────────────────── Profile / Storage webapi DTO ─────────────────── */

/**
 * GET /users/userinfo 响应 data（115 网页端「我的」页同源接口）。
 *
 *  关键字段：
 *    - userId   : Long     115 user_id（与扫码落 DataStore 的 uid 一致，用于交叉校验）
 *    - userName : String?  昵称（StorageCard 标题用）
 *    - userFace : String?  头像 URL（中等尺寸；Profile 屏暂不展示，先存）
 *
 *  115 实际回包可能还有 vip_info / rt_space_info 等冗余字段，
 *  [Json.ignoreUnknownKeys] 配在 [Cloud115ApiClient] 里会安全忽略。
 */
@Serializable
data class UserBaseInfoData(
    @SerialName("user_id") val userId: Long = 0L,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_face") val userFace: String? = null,
)

/** 单个尺寸项（all_total / files / rb 等的子结构）。 */
@Serializable
data class SizeInfo(
    val size: Double = 0.0,                   // 115 返回 Double（如 54410546868129.555）
    @SerialName("size_format") val sizeFormat: String = "",
    val percent: Double = 0.0,
    val count: Int? = null,                   // type_summury 里每类有 count
    val pecent: Double? = null,               // 115 typo: pecent 而非 percent（type_summury 里）
)

/**
 * POST /user/space_summury 响应 data（**注意 115 路径 + 部分字段都有拼写错误**）。
 *
 *  实际响应结构（实测）：
 *  ```
 *  {
 *    "state": true,
 *    "space_summury": {
 *      "all_total":  { "size": ..., "size_format": "49.49 TB" },
 *      "all_remain": { "size": ..., "size_format": "48.09 TB" },
 *      "files":      { "size": ..., "size_format": "1.33 TB" },
 *      "photo":      { "size": ..., "size_format": "59.11 GB" },
 *      "offine":     { ... },   // 115 typo: "offine" 而非 "offline"
 *      "note":       { ... },
 *      "rb":         { ... },   // rb = recycle bin 回收站
 *      "receive":    { ... },
 *    },
 *    "type_summury": {          // 按文件类型分布（8 类：RAR/EXE/DOC/MUS/PIC/AVI/BOOK/OTHER + 元数据）
 *      "RAR":  { "count": 20, "size": ..., "size_format": "1.58 GB" },
 *      "EXE":  { ... },
 *      ...
 *    },
 *    "rt_space_info": { ... }   // 实时空间（暂不展示）
 *  }
 *  ```
 *
 *  Profile 屏展示用的扁平字段由 [usedPct] / [totalLabel] / [remainingGb] / [trashGb]
 *  派生（VM 端 compute 后写入 ProfileUiState.Storage），不在这里预先映射。
 *
 *  设计 5 行 breakdown（视频/图片/文档/音频/其他）≠ 实际 8 类，breakdownsIsMock 标 true。
 */
@Serializable
data class SpaceSummuryData(
    @SerialName("space_summury") val spaceSummury: SpaceSummuryInner = SpaceSummuryInner(),
    /** 115 实际是混合 Map：RAR/EXE/DOC/... 是 SizeInfo 对象，
     *  `work_count_times` 是 Long，`type_nums` 是嵌套 Map。所以用 JsonElement 兼容。 */
    @SerialName("type_summury") val typeSummury: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class SpaceSummuryInner(
    @SerialName("all_total") val allTotal: SizeInfo = SizeInfo(),
    @SerialName("all_remain") val allRemain: SizeInfo = SizeInfo(),
    @SerialName("all_use") val allUse: SizeInfo = SizeInfo(),
    val files: SizeInfo = SizeInfo(),
    val photo: SizeInfo = SizeInfo(),
    @SerialName("offine") val offine: SizeInfo = SizeInfo(),   // 115 typo
    val note: SizeInfo = SizeInfo(),
    val rb: SizeInfo = SizeInfo(),                              // 回收站
    val receive: SizeInfo = SizeInfo(),
)
