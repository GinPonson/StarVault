package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 115 扫码登录 3 个端点。
 *
 *  base URL 在 [Cloud115ApiClient.scanRetrofit] 中配为 https://qrcodeapi.115.com/
 *
 *  ⚠️ 步骤 1/2/3 是 GET（115 习惯 query 而非 body），步骤 4 是 POST（loginResult）。
 */
interface ScanApiService {

    /** 拿扫码会话（uid / time / sign），返回包内的 qrcode 字段是跳转链接不是图。 */
    @GET("api/1.0/web/1.0/token/")
    suspend fun getScanToken(): Response<ApiEnvelope<QrTokenData>>

    /**
     * 轮询扫码状态。
     * @param sign 每轮要带，建立会话时拿到
     */
    @GET("get/status/")
    suspend fun checkScanStatus(
        @Query("uid") uid: String,
        @Query("time") time: Long,
        @Query("sign") sign: String,
    ): Response<ApiEnvelope<QrStatusData>>

    /**
     * 扫码确认后绑定设备并拿 cookies。
     *
     *  cookies 在响应 body 的 `data.cookie` 字符串里（**不是 Set-Cookie 头**），
     *  Lumen/p115client 都从这里读。
     *
     *  @param app 路径变量 + 表单 app/appname，决定 ssoent：
     *    - "web"       → ssoent=A1（踢用户 115.com 网页端 session）
     *    - "qandroid"  → ssoent=M1（115管理 app；不踢 web/115 Android App）
     *    - "alipaymini"/"wechatmini" → 小程序 ssoent
     *  默认 "qandroid"：与用户既有 web / 115 Android App session 互不干扰。
     */
    @FormUrlEncoded
    @POST("app/1.0/{app}/1.0/login/qrcode/")
    suspend fun getLoginResult(
        @Path("app") app: String = "qandroid",
        @Field("app") appName: String = "qandroid",
        @Field("account") account: String,         // = QR session uid
    ): Response<ApiEnvelope<LoginResultData>>
}
