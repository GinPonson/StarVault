package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
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
    @GET("api/1.0/web/1.0/qrcode/")
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
     * 扫码确认后取 cookies + 扫码用户信息。
     * cookies 在 Response Headers Set-Cookie 里，**不在 body**。
     * body 里的 data 是 LoginResultData（userId/userName/device）。
     *
     *  app=qandroid → ssoent=M1（115管理），不踢用户 web (A1) / 115 安卓 App (F3) session。
     */
    @FormUrlEncoded
    @POST("app/1.0/qandroid/1.0/login/qrcode/")
    suspend fun getLoginResult(
        @Field("app") app: String = "qandroid",
        @Field("appname") appName: String = "qandroid",
        @Field("account") account: String,         // = uid
    ): Response<ApiEnvelope<LoginResultData>>
}
