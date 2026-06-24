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
 *  - open 域（qrcodeapi.115.com/open/...）：`state: 0/1` (Int)
 *
 *  字段差异（webapi 用 `error` + `errno` 表示失败，open 用 `code` + `message`）：
 *  - message : open 失败文本
 *  - error   : webapi 失败文本
 *  - code    : open 错误码
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
 *  使用：[com.starvault.data.remote.cloud115.OpenAuthManager] open 域、
 *         [com.starvault.data.repository.AuthRepository] webapi 域都通过此判定。
 */
val ApiEnvelope<*>.isOk: Boolean
    get() {
        val s = state as? JsonPrimitive ?: return false
        return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
    }

// ─────────────────── User DTO (OAuth /open/user/info) ───────────────────

/**
 * GET /open/user/info 响应 data（OAuth Bearer 鉴权专用）。
 *
 *  字段对照官方文档(https://www.yuque.com/115yun/open/ot1litggzxa1czww):
 *  - userId       : string   115 user_id(OAuth 登录响应里也有,与 DataStore.uid 一致)
 *  - userName     : string   昵称
 *  - userFaceS/M/L: string   三档头像 URL(暂不展示,先解析留作后续 Settings 用)
 *  - rtSpaceInfo  : object   实时空间(all_total/all_remain/all_use)— Profile 屏容量条用
 *  - vipInfo      : object   VIP 等级 + 过期(Profile 屏右上角徽章用)
 *
 *  其它冗余字段由 [Json.ignoreUnknownKeys] 安全忽略。
 */
@Serializable
data class UserBaseInfoData(
    @SerialName("user_id") val userId: Long = 0L,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_face_s") val userFaceS: String? = null,
    @SerialName("user_face_m") val userFaceM: String? = null,
    @SerialName("user_face_l") val userFaceL: String? = null,
    @SerialName("rt_space_info") val rtSpaceInfo: RtSpaceInfo? = null,
    @SerialName("vip_info") val vipInfo: VipInfo? = null,
)

/**
 * 实时空间概要（来自 /open/user/info 的 rt_space_info 节点）。
 *
 *  实测响应（2026-06 AVD 真机 + 媒体播放器 AppID）：
 *  ```
 *  "rt_space_info": {
 *    "all_total":  { "size": 54410546868129, "size_format": "49.49TB" },
 *    "all_remain": { "size": 52831660016420, "size_format": "48.05TB" },
 *    "all_use":    { "size":  1578886851709, "size_format": "1.44TB" }
 *  }
 *  ```
 *
 *  取代旧的 webapi POST /user/space_summury（OAuth Open 平台未开放 type_summury 8 类细分）。
 *  Profile 屏容量条用 `all_total` / `all_remain`；VIP 徽章用 [VipInfo]。
 */
@Serializable
data class RtSpaceInfo(
    @SerialName("all_total")  val allTotal: SizeInfo = SizeInfo(),
    @SerialName("all_remain") val allRemain: SizeInfo = SizeInfo(),
    @SerialName("all_use")    val allUse: SizeInfo = SizeInfo(),
)

/** VIP 等级 + 过期时间戳（来自 /open/user/info 的 vip_info 节点）。
 *
 *  字段对照官方文档(https://www.yuque.com/115yun/open/ot1litggzxa1czww):
 *  - level_name : string  原石会员 / 尝鲜VIP / 体验VIP / 月费VIP / 年费VIP / 年费VIP高级版 / 年费VIP特级版 / 超级VIP / 长期VIP
 *  - expire     : int     过期时间戳(unix 秒);用 Int 装(2038 年前够用),负数或 0 = 永久/无数据
 */
@Serializable
data class VipInfo(
    @SerialName("level_name") val levelName: String? = null,
    val expire: Int? = null,
)

/**
 * 单个尺寸项（all_total / all_remain / all_use 的子结构）。
 *
 *  proapi /open/user/info 返回的 size 是 Long 类型整数（不是 Double）。
 *  用 Long 存，避免 49.49TB 转 Double 精度丢失。
 */
@Serializable
data class SizeInfo(
    val size: Long = 0L,
    @SerialName("size_format") val sizeFormat: String = "",
)