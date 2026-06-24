package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 115 OAuth 设备码流的状态轮询端点。
 *
 * 与 [OpenAuthApiService] 共享 baseUrl `https://qrcodeapi.115.com/`，但用独立的
 * [okhttp3.OkHttpClient]（见 [Cloud115ApiClient.buildLongPollOkHttpClient]），
 * 把 read timeout 拉到 65s（115 长轮询会挂 30~60s）。
 *
 * 端点契约（GET）：
 *  `https://qrcodeapi.115.com/get/status/?uid=<uid>&time=<time>&sign=<sign>&_=<tsMillis>`
 *
 * 返回 `data` 形状见 [StatusPollResponse]：
 *  - `status=0` 用户未扫 / 未确认 → 继续轮询
 *  - `status=1` 已扫未确认 → emit Scanned 一次
 *  - `status=2` 已确认 → 触发 `deviceCodeToToken` 换真 token
 *
 * `_=...` cacheBuster 必加：GET 不带 cacheBuster 时 115 偶尔返回缓存的 status=0。
 *
 * @param uid   来自 [DeviceCodeResponse.uid]
 * @param time  来自 [DeviceCodeResponse.time]（服务端时间，秒）
 * @param sign  来自 [DeviceCodeResponse.sign]
 * @param cacheBuster 客户端 `System.currentTimeMillis()`，防缓存
 */
interface StatusPollApi {

    @GET("get/status/")
    suspend fun getStatus(
        @Query("uid") uid: String,
        @Query("time") time: Long,
        @Query("sign") sign: String,
        @Query("_") cacheBuster: Long,
    ): Response<ApiEnvelope<StatusPollResponse>>
}