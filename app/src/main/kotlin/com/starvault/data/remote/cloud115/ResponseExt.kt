package com.starvault.data.remote.cloud115

import retrofit2.Response

/**
 * 校验 HTTP 层成功 + body 非空,否则抛 IllegalStateException。
 *
 * 集中解决仓库层的重复 boilerplate:
 *  `if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")`
 *  `val body = resp.body() ?: throw IllegalStateException("empty body")`
 *
 * 业务失败(state=false / errno != 0)由调用方拿到 body 后用 [isOk] 或 state 字段判。
 *
 * @throws IllegalStateException HTTP code 非 2xx,或 body 为 null
 */
fun <T> Response<T>.requireSuccessful(): T {
    if (!isSuccessful) throw IllegalStateException("HTTP ${code()}")
    return body() ?: throw IllegalStateException("empty body")
}