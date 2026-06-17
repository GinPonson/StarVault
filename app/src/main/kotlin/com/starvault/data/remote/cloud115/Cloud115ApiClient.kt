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
 *  - SCAN_BASE_URL : qrcodeapi.115.com/  (扫码 + 后续 OpenAPI)
 *  - WEB_BASE_URL  : webapi.115.com/     (用户信息 / 空间 / 文件列表等)
 *
 *  共用一个 OkHttpClient：30s 超时 + 浏览器伪装 UA/Referer/Origin + Cookie 注入。
 *  Cookie 由 [CookieInterceptor] 在请求时从 [com.starvault.data.local.auth.Cloud115AuthStore] 实时读。
 *
 *  使用方式：每个 base URL 一个 [Retrofit] 实例，但 [OkHttpClient] 复用
 *  （拦截器 / 连接池 / 线程池共享）。
 */
object Cloud115ApiClient {

    const val SCAN_BASE_URL = "https://qrcodeapi.115.com/"
    const val WEB_BASE_URL  = "https://webapi.115.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun buildOkHttpClient(cookieProvider: () -> String?): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(browserLikeHeaderInterceptor())
            .addInterceptor(CookieInterceptor(cookieProvider))
            .build()

    fun scanRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(SCAN_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun scanApiService(cookieProvider: () -> String?): ScanApiService =
        scanRetrofit(buildOkHttpClient(cookieProvider)).create(ScanApiService::class.java)

    /**
     * webapi 域 Retrofit 工厂（用户信息 / 空间 / 后续文件列表）。
     *
     * 共用同一个 [OkHttpClient]（浏览器伪装头 + Cookie 注入），仅替换 baseUrl。
     *  [UserApiService] 由 [webApiService] 在创建接口实例时调用。
     */
    fun webRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(WEB_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** webapi 域 [UserApiService] 工厂（用户信息 / 空间概要）。 */
    fun userApiService(cookieProvider: () -> String?): UserApiService =
        webRetrofit(buildOkHttpClient(cookieProvider)).create(UserApiService::class.java)

    /**
     * 浏览器伪装头：Referer/Origin/User-Agent 全用 115 域名，
     * 否则 115 部分端点返回 state=false + 跨域拦截。
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
