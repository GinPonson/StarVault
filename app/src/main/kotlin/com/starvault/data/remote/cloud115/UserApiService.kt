package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 115 webapi 用户域端点（base URL = webapi.115.com/）。
 *
 *  用途：Profile 屏「云端空间」卡 / 后续 Settings 屏拉真数据。
 *  共用 [ScanApiService] 的 [Cloud115ApiClient.buildOkHttpClient]（含 Cookie 注入）。
 *
 *  所有端点都需登录态（Cookie 头由 [CookieInterceptor] 注入）。
 *  401（cookie 失效）暂不处理，先让 Profile 屏展示「未登录」/ mock 态。
 */
interface UserApiService {

    /**
     * GET /users/userinfo — 拉用户基础信息（uid / 昵称 / 头像）。
     *
     *  对应 115 网页端「我的」页同源接口，cookies 注入后正常返回。
     *  无 cookies 时 115 返回 state=false + errno=990001（登录超时）。
     */
    @GET("users/userinfo")
    suspend fun getUserBaseInfo(): Response<ApiEnvelope<UserBaseInfoData>>

    /**
     * POST /user/space_summury — 拉容量概要（**注意 115 路径的拼写错误 summury**）。
     *
     *  **必须是 POST**：实测 GET 返回 405 METHOD NOT ALLOWED + errno=980005。
     *
     *  返回结构（实测）：
     *  ```
     *  {
     *    "state": true,
     *    "space_summury": { "all_total": {...}, "files": {...}, "rb": {...}, ... },
     *    "type_summury":  { "RAR": {...}, "EXE": {...}, "DOC": {...}, ... },
     *    "rt_space_info": { ... }
     *  }
     *  ```
     *  注：响应**不在 `data` 字段下**，而 `space_summury` / `type_summury` 是顶层 key。
     *  所以用独立的 [SpaceSummuryResponse] 而不是 [ApiEnvelope]，免得 115 跟 scan 域
     *  共用 envelope 但响应 shape 不一样导致误判。
     */
    @POST("user/space_summury")
    suspend fun getSpaceSummury(): Response<SpaceSummuryResponse>
}

/**
 * POST /user/space_summury 实际响应 shape（**顶层就是数据**，没有 `data` 包装）。
 *
 *  state 字段类型与 [ApiEnvelope] 一致（115 webapi 用 true/false Boolean），
 *  复用相同的 [isOk] 判定逻辑（实际为本地扩展函数 [SpaceSummuryResponse.isOk]）。
 */
@Serializable
data class SpaceSummuryResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("space_summury") val spaceSummury: SpaceSummuryInner = SpaceSummuryInner(),
    /** 115 实际是混合 Map：RAR/EXE/DOC/... 是 SizeInfo 对象，
     *  `work_count_times` 是 Long，`type_nums` 是嵌套 Map。所以用 JsonElement 兼容。 */
    @SerialName("type_summury") val typeSummury: Map<String, JsonElement> = emptyMap(),
) {
    /** 业务成功判定：state=true (Boolean) 或 state=1 (Int)。与 [ApiEnvelope.isOk] 一致语义。 */
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}
