package com.starvault.data.remote.cloud115

import okhttp3.Interceptor

/**
 * 注入 115 cookies 到每个请求的 Cookie 头。
 *
 * cookies 由 [cookieProvider] 在请求时实时提供（DataStore Flow.first() + runBlocking）。
 * cookies 为空时不加 Cookie 头（避免给未登录请求发无意义 header）。
 */
internal class CookieInterceptor(private val cookieProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val cookies = cookieProvider()
        val req = if (cookies.isNullOrBlank()) chain.request()
        else chain.request().newBuilder().header("Cookie", cookies).build()
        return chain.proceed(req)
    }
}
