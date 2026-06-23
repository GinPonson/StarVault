package com.starvault.data.remote.cloud115

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 注入 115 OAuth Bearer token 到所有请求的 Authorization 头。
 *
 * 替换 [CookieInterceptor]（旧的 Cookie 注入拦截器）。
 *
 * token 由 [tokenProvider] 实时提供（DataStore Flow.first() + runBlocking）。
 * token 为空时**不加** Authorization 头，避免给未登录请求发无意义 header。
 *
 * 共用一个 OkHttpClient：承担 `webapi.115.com` + `qrcodeapi.115.com` + 115 CDN 三种 host。
 *  - webapi / qrcodeapi/open : token 是必须的，否则 401
 *  - 115 CDN                  : token 多余无害，签名 URL + Referer/Origin/UA 才是关键
 *
 * 没有 401 retry / refresh-on-401（不在本期范围；失败 → 用户掉登录 → 重扫）。
 *
 * @param tokenProvider 同步取 token 的 lambda，运行时从 [OpenAuthStore.accessTokenBlocking] 注入
 */
internal class AuthHeaderInterceptor(
    private val tokenProvider: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        // 未登录 / token 为空：原样转发，避免给没必要的请求加 Authorization 头
        // 已登录：注入 `Authorization: Bearer <token>`，115 后端会据此识别会话
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}