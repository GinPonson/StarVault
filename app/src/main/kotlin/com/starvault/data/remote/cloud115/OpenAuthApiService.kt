package com.starvault.data.remote.cloud115

import kotlinx.serialization.json.JsonElement
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
 * PKCE 形态（对齐 OpenList 115-sdk-go auth.go:21-22 — `calCodeChanllenge`）：
 *  - `client_id`             固定 `100195125`（媒体播放器）
 *  - `code_challenge`        `base64(sha256(verifier))` = `YOBb0bGVry+UES+nGXpciCiQWIQM58bflpN1a8YlD1U=`
 *  - `code_challenge_method` = `sha256`
 *  - `code_verifier`         换 token 时回传 `"0"*64`
 *  - secret 永不入 DataStore：只在 [OpenAuthManager] 内单次会话内传递
 *
 * refresh 端点走 `passportapi.115.com/open/refreshToken`(115-sdk-go const.go:9-12),
 * **不**走 proapi(`open/authTokenRefresh` 是 p115client 旧路径,proapi 上 115 不一定代理)。
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
        @Field("code_challenge_method") method: String = "sha256",
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
     * 响应 type 用 [JsonElement](**非** [TokenRefreshResponse]) — passportapi refresh 失败时
     * `data` 字段是 `[]`(空数组),strict kotlinx-serialization 反序列化会抛
     * "Expected '{' but had '['"。[Token401Interceptor] 拿到 [JsonElement] 后手动判断:
     *  - 业务成功 + data 是 object → 解析 [TokenRefreshResponse] 取新 at/rt
     *  - 业务失败 / data 是 array → 视为 refresh 失败
     *
     * 端点实际位置:**passportapi.115.com/open/refreshToken**(对齐 OpenList 115-sdk-go const.go:9-12)。
     * refresh 端点走独立 Retrofit 实例,baseUrl = `https://passportapi.115.com/`,与设备码/换 token
     * 用的 qrcodeapi 不同。
     */
    @FormUrlEncoded
    @POST("open/refreshToken")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
    ): Response<ApiEnvelope<JsonElement>>

    /**
     * 服务端吊销 refresh_token。
     *
     * signOut 流程调用。失败由调用方吞掉（fire-and-forget）。
     *
     * 端点走 passportapi 域(与 refreshToken 同源);client_id 不传,只传 refresh_token。
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

        /** PKCE verifier("0"*64),换 token 时回传。 */
        const val CODE_VERIFIER =
            "00000000000000000000000000000000" +
            "00000000000000000000000000000000"

        /**
         * PKCE challenge = base64(sha256(CODE_VERIFIER))。
         *
         * 对齐 OpenList 115-sdk-go auth.go:21-22 `calCodeChanllenge`(sha256,不是 md5)。
         * server 端验签时也算 sha256 跟 md5(verifier) 比对,二者必须算法一致。
         */
        const val CODE_CHALLENGE = "YOBb0bGVry+UES+nGXpciCiQWIQM58bflpN1a8YlD1U="
    }
}