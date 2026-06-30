package com.starvault.data.preview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 5min 签名 downurl 的内存缓存 — 对齐 OpenList `drivers/115_open` 的 `LinkCacheUA` 思路,
 * 但只按 fileId 单维度缓存(StarVault UA 由 `browserLikeHeader` 注入固定,不需 UA 维度)。
 *
 * **背景**:115 `/open/ufile/downurl` 端点返回 5min 签名直链;同 fileId 5min 内重复请求
 * 拿到的 URL 完全等价。Preview 屏每次进入都重新 fetch 是浪费一次 proapi 往返(用户切回
 * PreviewImage 屏时尤其明显)。
 *
 * **生命周期**:
 *  - 进程内单例 ([ServiceLocator.downUrlCache] 持有) — VM pop / nav back 不清缓存,
 *    用户来回切 Preview 屏受益。
 *  - 不持久化 — 5min 签名过完自然失效,下次 fetch 拿新 URL。
 *
 * **串行化**:[Mutex] 防并发 miss 时 N 个协程同时触发 fetcher,造成 N 次重复请求;
 * 双重检查(double-checked locking 模式)减少锁内 await。
 *
 * **不强制 invalidate**:CRUD(删 / 移 / 改名)后,bad URL 命中只持续 ≤4min;fid 被
 * 删 → 缓存 orphan 自然 GC;fid 移走 → URL 仍指向同一 115 文件(115 不换 URL),所以缓存
 * 不会被 stale 命中。
 *
 * @param maxAgeMs TTL(ms),默认 4min — 留 1min buffer 让缓存自然失效重 fetch
 */
class DownUrlCache(private val maxAgeMs: Long = DEFAULT_TTL_MS) {

    private data class Entry(val url: String, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Entry>()
    private val mutex = Mutex()

    /**
     * 取 cache[fid];miss / 过期则串行调 [fetcher],缓存结果后返回。
     *
     * @param fileId 115 文件 id(跟 OpenList `[obj.GetID()]` 一致)
     * @param fetcher 实际网络层 — 失败返回 Result.failure;不写缓存
     * @return 命中或新 fetch 的 URL 字符串 Result;或 fetcher 失败的 Result.failure
     */
    suspend fun getOrFetch(
        fileId: String,
        fetcher: suspend () -> Result<String>,
    ): Result<String> {
        // 快速路径:不取锁读缓存,避免单协程 miss 时多个 goroutine 抢锁
        cache[fileId]?.let { entry ->
            if (entry.expiresAt > System.currentTimeMillis()) {
                return Result.success(entry.url)
            }
        }
        return mutex.withLock {
            // 双重检查:别的协程可能刚 fetch 完
            cache[fileId]?.let { entry ->
                if (entry.expiresAt > System.currentTimeMillis()) {
                    return@withLock Result.success(entry.url)
                }
            }
            fetcher().onSuccess { url ->
                cache[fileId] = Entry(url, System.currentTimeMillis() + maxAgeMs)
            }
        }
    }

    /** 显式失效单条目 — 预留 hook,未来 CRUD 后调用(目前未用,见 KDoc)。 */
    @Suppress("unused")
    fun invalidate(fileId: String) {
        cache.remove(fileId)
    }

    /** 全清 — 重新登录 / token 刷新时可调。 */
    @Suppress("unused")
    fun clear() {
        cache.clear()
    }

    companion object {
        /** 默认 TTL:4min(115 downurl 签名 5min,留 1min buffer)。 */
        const val DEFAULT_TTL_MS: Long = 4L * 60 * 1000
    }
}
