package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 115 开放平台 OAuth 设备码流的 Retrofit 接口（业务端点）。
 *
 * 状态轮询端点（`get/status/`）独立成 [StatusPollApi]，
 * 因为它需要 65s read timeout，不能复用这里的 30s client。
 * 两个接口共享 baseUrl `https://qrcodeapi.115.com/`，仅超时策略不同。
 *
 * 三步流程：
 *  1. POST open/authDeviceCode       → 拿 uid/time/sign/qrcode（[requestDeviceCode]）
 *  2. GET  get/status/  (long-poll)  → status==2 时继续；见 [StatusPollApi]
 *  3. POST open/deviceCodeToToken    → 换真 access_token / refresh_token（[deviceCodeToToken]）
 *
 * PKCE 形态（与 p115client `_default_code_*` 一致，见 p115client/const.py:73-76）：
 *  - `client_id`             固定 `100195125`（媒体播放器）
 *  - `code_challenge`        固定 `md5("0"*64) = EOq2AI1WQs9Cq9KqQfhHyw==`
 *  - `code_challenge_method` = `md5`
 *  - `code_verifier`         换 token 时回传 `"0"*64`
 *  - secret 永不入 DataStore：只在 [OpenAuthManager] 内单次会话内传递
 */
interface OpenAuthApiService {

    /**
     * 申请设备码。
     *
     * 表单字段（client_id/code_challenge/method）走默认值，调用方无需手动传。
     * @return 成功 → data.uid/time/qrcode/sign 给轮询与 QR 渲染用
     */
    @FormUrlEncoded
    @POST("open/authDeviceCode")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("code_challenge") codeChallenge: String = CODE_CHALLENGE,
        @Field("code_challenge_method") method: String = "md5",
    ): Response<ApiEnvelope<DeviceCodeResponse>>

    /**
     * 用设备码换真 access_token（status==2 后调用）。
     *
     * @param uid          来自 [DeviceCodeResponse.uid]
     * @param codeVerifier PKCE verifier（固定 `"0"*64`，调用方传 [CODE_VERIFIER]）
     * @return 成功 → data.access_token / refresh_token / user_id / user_name
     */
    @FormUrlEncoded
    @POST("open/deviceCodeToToken")
    suspend fun deviceCodeToToken(
        @Field("uid") uid: String,
        @Field("code_verifier") codeVerifier: String = CODE_VERIFIER,
    ): Response<ApiEnvelope<DeviceCodeToTokenResponse>>

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

        /** md5("0"*64) 对应 verifier。 */
        const val CODE_VERIFIER =
            "00000000000000000000000000000000" +
            "00000000000000000000000000000000"

        /** MD5(CODE_VERIFIER) 的 base64 表达 — 115 PKCE 用法。 */
        const val CODE_CHALLENGE = "EOq2AI1WQs9Cq9KqQfhHyw=="
    }
}