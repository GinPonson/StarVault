package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 115 开放平台 OAuth 设备码流（device-code flow）的 Retrofit 接口。
 *
 * 替换 [ScanApiService] 的 3 个扫码端点。
 * base URL 在 [Cloud115ApiClient.openAuthRetrofit] 中配为 https://qrcodeapi.115.com/
 * （与旧的 SCAN_BASE_URL 同域，URL 复用一个域名）。
 *
 * PKCE 形态：
 *  - `client_id`           固定 `100195125`（媒体播放器）
 *  - `code_challenge`      固定 `md5("0"*64) = EOq2AI1WQs9Cq9KqQfhHyw==`
 *  - `code_challenge_method` = `md5`（与 p115client 默认一致）
 *  - `code_verifier`       轮询时回传 `"0"*64`
 *  - secret 永不入 DataStore：只在 [OpenAuthManager] 内单次会话内传递
 */
interface OpenAuthApiService {

    /**
     * 申请设备码。
     *
     * 表单字段（client_id/code_challenge/method）走默认值，调用方无需手动传。
     * @return 成功 → data.uid / qrcode / sign 给轮询与 QR 渲染用
     */
    @FormUrlEncoded
    @POST("open/authDeviceCode")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("code_challenge") codeChallenge: String = CODE_CHALLENGE,
        @Field("code_challenge_method") method: String = "md5",
    ): Response<ApiEnvelope<DeviceCodeResponse>>

    /**
     * 轮询扫码状态。
     *
     * 用户在 115 App 扫码 + 点确认之前 → data 为 null 或 access_token 缺失；
     * 用户确认后 → data.access_token 存在 → 终止轮询。
     *
     * @param uid          来自 requestDeviceCode.data.uid
     * @param sign         来自 requestDeviceCode.data.sign
     * @param codeVerifier 永不入库，写死 "0"*64
     */
    @GET("open/authDeviceCode")
    suspend fun pollDeviceCode(
        @Query("uid") uid: String,
        @Query("sign") sign: String,
        @Query("code_verifier") codeVerifier: String = CODE_VERIFIER,
    ): Response<ApiEnvelope<TokenPollResponse>>

    /**
     * 刷新 access_token。
     *
     * 本期不接，留接口位供 Phase 2 自动续期使用。
     */
    @FormUrlEncoded
    @POST("open/authTokenRefresh")
    suspend fun refreshToken(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("refresh_token") refreshToken: String,
    ): Response<ApiEnvelope<TokenRefreshResponse>>

    /**
     * 服务端吊销 refresh_token。
     *
     * signOut 流程调用。失败由调用方吞掉（fire-and-forget）。
     */
    @FormUrlEncoded
    @POST("open/authTokenRevoke")
    suspend fun revokeToken(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("refresh_token") refreshToken: String,
    ): Response<ApiEnvelope<TokenRevokeResponse>>

    companion object {
        /** 媒体播放器 open 应用 — 2026-06 实测可用。 */
        const val CLIENT_ID = "100195125"

        /** md5("0"*64) = EOq2AI1WQs9Cq9KqQfhHyw==，对应 code_verifier = "0"*64。 */
        const val CODE_VERIFIER =
            "00000000000000000000000000000000" +
            "00000000000000000000000000000000"

        /** MD5(CODE_VERIFIER) 的 base64 表达 — 115 PKCE 用法。 */
        const val CODE_CHALLENGE = "EOq2AI1WQs9Cq9KqQfhHyw=="
    }
}