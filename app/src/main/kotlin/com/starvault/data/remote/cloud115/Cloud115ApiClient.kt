package com.starvault.data.remote.cloud115

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 115 网盘 HTTP 客户端工厂。
 *
 *  2 个 base URL（按用途分）：
 *  - OPEN_AUTH_BASE_URL : qrcodeapi.115.com/  (OAuth 设备码 + 旧扫码域同源)
 *  - WEB_BASE_URL       : webapi.115.com/     (用户信息 / 空间 / 文件列表等)
 *
 *  共用一个 OkHttpClient：30s 超时 + 浏览器伪装 UA/Referer/Origin + OAuth Bearer 注入。
 *  Bearer token 由 [AuthHeaderInterceptor] 在请求时从 [com.starvault.data.local.auth.OpenAuthStore] 实时读。
 *
 *  使用方式：每个 base URL 一个 [Retrofit] 实例，但 [OkHttpClient] 复用
 *  （拦截器 / 连接池 / 线程池共享）。
 *
 *  替换历史：[CookieInterceptor] → [AuthHeaderInterceptor]；[scanApiService] → [openAuthApiService]。
 */
object Cloud115ApiClient {

    /** OAuth 设备码端点（与旧扫码域同源，URL 复用）。 */
    const val OPEN_AUTH_BASE_URL = "https://qrcodeapi.115.com/"

    /** webapi 域：用户信息 / 空间 / 文件列表。 */
    const val WEB_BASE_URL = "https://webapi.115.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * 构建共享 OkHttpClient，注入 Bearer token（[AuthHeaderInterceptor]）。
     *
     * @param tokenProvider 同步 lambda，从 [OpenAuthStore.accessTokenBlocking] 注入；
     *                       未登录时返回 null（不注入 Authorization 头）
     */
    fun buildOkHttpClient(tokenProvider: () -> String?): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(browserLikeHeaderInterceptor())
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))
            .build()

    /**
     * open 域 Retrofit 工厂（OAuth 设备码 [OpenAuthApiService]）。
     */
    fun openAuthRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_AUTH_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** open 域 [OpenAuthApiService] 工厂（独立 client，用于单元测试）。 */
    fun openAuthApiService(tokenProvider: () -> String?): OpenAuthApiService =
        openAuthRetrofit(buildOkHttpClient(tokenProvider)).create(OpenAuthApiService::class.java)

    /** open 域 [OpenAuthApiService] 工厂（共享 OkHttpClient，ServiceLocator 用）。 */
    fun openAuthApiService(client: OkHttpClient): OpenAuthApiService =
        openAuthRetrofit(client).create(OpenAuthApiService::class.java)

    /**
     * webapi 域 Retrofit 工厂（用户信息 / 空间 / 文件列表）。
     *
     * 共用同一个 [OkHttpClient]（浏览器伪装头 + Bearer 注入），仅替换 baseUrl。
     */
    fun webRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(WEB_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** webapi 域 [UserApiService] 工厂（独立 client）。 */
    fun userApiService(tokenProvider: () -> String?): UserApiService =
        webRetrofit(buildOkHttpClient(tokenProvider)).create(UserApiService::class.java)

    /** webapi 域 [UserApiService] 工厂（共享 OkHttpClient）。 */
    fun userApiService(client: OkHttpClient): UserApiService =
        webRetrofit(client).create(UserApiService::class.java)

    /** webapi 域 [FileApiService] 工厂（独立 client）。 */
    fun fileApiService(tokenProvider: () -> String?): FileApiService =
        webRetrofit(buildOkHttpClient(tokenProvider)).create(FileApiService::class.java)

    /** webapi 域 [FileApiService] 工厂（共享 OkHttpClient）。 */
    fun fileApiService(client: OkHttpClient): FileApiService =
        webRetrofit(client).create(FileApiService::class.java)

    /**
     * 浏览器伪装头：Referer/Origin/User-Agent 全用 115 域名，
     * 否则 115 部分端点返回 state=false + 跨域拦截。
     *
     * 注：CDN 缩略图 URL（*.115cdn.com 等）依赖签名参数 + Referer/Origin/UA，
     * Bearer 多余但无害（[AuthHeaderInterceptor] 一并注入）。
     */
    private fun browserLikeHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .header("Referer", "https://115.com/")
            .header("Origin", "https://115.com")
            .build()
        chain.proceed(req)
    }
}