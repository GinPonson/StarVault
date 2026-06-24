package com.starvault.data.remote.cloud115

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET

/**
 * 115 proapi open 域用户端点(base URL = proapi.115.com/)。
 *
 * OAuth Bearer 鉴权专用。响应顶层 `{state, data:{user_id, user_name, rt_space_info, vip_info, ...}, ...}`,
 * 解析后直接得到 [UserBaseInfoData]。
 *
 * 参考：https://www.yuque.com/115yun/open/ot1litggzxa1czww(官方 Open Platform 文档「用户信息」)
 */
interface OpenUserApiService {

    /**
     * GET /open/user/info — 当前登录用户基本信息 + 空间概要 + VIP。
     *
     * 响应字段(参考官方文档 + AVD 2026-06 实测):
     *  - state : Boolean, true=成功
     *  - data.user_id / user_name / user_face_{s,m,l} : 身份
     *  - data.rt_space_info : {all_total, all_remain, all_use}(实时空间)
     *  - data.vip_info      : {level_name, expire}(VIP 等级)
     *  - 其它字段 : ignoreUnknownKeys 安全忽略
     */
    @GET("open/user/info")
    suspend fun userInfo(): Response<OpenUserInfoResponse>
}

/**
 * GET /open/user/info 响应 shape(顶层就是数据,没有 `data` 包装)。
 *
 * 用具体类型 [UserBaseInfoData] 替代 JsonElement,kotlinx-serialization 自动
 * 嵌套解析;`ignoreUnknownKeys=true` 兜底 115 偶发加字段。
 */
@Serializable
data class OpenUserInfoResponse(
    val state: Boolean? = null,
    val error: String? = null,
    val errno: Int? = null,
    @kotlinx.serialization.SerialName("errNo") val errNo: Int? = null,
    val message: String? = null,
    val data: UserBaseInfoData? = null,
)
