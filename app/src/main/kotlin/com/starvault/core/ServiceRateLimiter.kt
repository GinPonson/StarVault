package com.starvault.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 简易 1r/s 限速器 — 对齐 OpenList `Addition.LimitRate` 默认 1r/s,防恶意循环 / 异常路径
 * 高频调用触发 115 开放平台风控。
 *
 * **算法**:mutex + lastTimestamp — 每次 [acquire] 检查 `now - lastTimestamp`,
 * 不足 [intervalMs] 则 delay 余下部分;**不允许 burst**(简化)— StarVault 单用户场景
 * 没用到 burst,够用。
 *
 * **不是** OkHttp Interceptor(用 `runBlocking` 阻塞线程池反模式,会让 WorkManager
 * 并发下载全部串行等);
 * **是** Repository 层 wrap — 所有 suspend API 入口 `acquire { api.xx() }`,串行获取在
 * 协程而非线程层,不污染 OkHttp 线程池。
 *
 * **测试用法**:构造时传大值 `ServiceRateLimiter(permitsPerSecond = 1000.0)`,intervalMs
 * 趋近 0,测试不真限速。
 *
 * @param permitsPerSecond 默认 1.0(对齐 OpenList 默认)
 */
class ServiceRateLimiter(
    private val permitsPerSecond: Double = DEFAULT_PERMITS_PER_SECOND,
) {
    private val intervalMs: Long = if (permitsPerSecond > 0) {
        (1000.0 / permitsPerSecond).toLong().coerceAtLeast(0L)
    } else {
        Long.MAX_VALUE
    }
    private val mutex = Mutex()
    private var lastTimestamp: Long = 0L

    /**
     * 串行获取 permit 后执行 [block]。
     *
     * 使用模式(Repository suspend 公开方法):
     * ```kotlin
     * suspend fun listFolder(...): Result<...> =
     *     serviceRateLimiter.acquire {
     *         runCatching { api.listFiles(...).requireSuccessful() }
     *     }
     * ```
     */
    suspend fun <T> acquire(block: suspend () -> T): T = mutex.withLock {
        if (intervalMs > 0L) {
            val now = System.currentTimeMillis()
            val delta = now - lastTimestamp
            if (delta < intervalMs) {
                delay(intervalMs - delta)
            }
            lastTimestamp = System.currentTimeMillis()
        }
        block()
    }

    companion object {
        /** 默认速率:1 req/sec — 对齐 OpenList `Addition.LimitRate` 默认值。 */
        const val DEFAULT_PERMITS_PER_SECOND: Double = 1.0
    }
}
