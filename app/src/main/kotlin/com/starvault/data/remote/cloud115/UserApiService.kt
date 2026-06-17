package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.GET

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
     *  无 cookies 时 115 返回 state=0 + errno=990001（登录超时）。
     */
    @GET("users/userinfo")
    suspend fun getUserBaseInfo(): Response<ApiEnvelope<UserBaseInfoData>>

    /**
     * GET /user/space_summury — 拉容量概要（**注意 115 路径的拼写错误 summury**）。
     *
     *  字节数为单位（[SpaceSummuryData.allSpace] / [SpaceSummuryData.usedSpace] 等），
     *  调用方按 1 GB = 1024^3 bytes 换算。
     */
    @GET("user/space_summury")
    suspend fun getSpaceSummury(): Response<ApiEnvelope<SpaceSummuryData>>
}
