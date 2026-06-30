package com.starvault.data.repository

import com.starvault.core.ServiceRateLimiter
import com.starvault.data.preview.DownUrlCache
import com.starvault.data.remote.cloud115.OpenDownUrlItem
import com.starvault.data.remote.cloud115.OpenDownUrlResponse
import com.starvault.data.remote.cloud115.OpenDownUrlUrl
import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.OpenFileUpdateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * MediaPreviewRepository 单元测试 — 聚焦 downurl 缓存行为(M6)+ setStar 走限速常规测试。
 *
 * 核心目的:验证 [com.starvault.data.preview.DownUrlCache] 在 Repository 层正确 wire,
 * 同 fileId 第二次请求不走 api.downloadUrl(proapi 节省 1 次往返)。
 *
 * 测试 fixture:用 `ServiceRateLimiter(permitsPerSecond = 1000.0)` 跳过限速(`intervalMs` ≈ 1ms
 * 不阻塞 runTest 内部 `delay` 调度),用全新 `DownUrlCache()` 实例避免测试间缓存污染。
 */
class MediaPreviewRepositoryTest {

    private val api = mockk<OpenFileApiService>()
    private val cache = DownUrlCache()
    private val noopLimiter = ServiceRateLimiter(permitsPerSecond = 1000.0)
    private val repo = MediaPreviewRepository(api, cache, noopLimiter)

    // ─────────────────── fetchImageOriginalUrl 缓存 ───────────────────

    @Test
    fun `fetchImageOriginalUrl returns URL on success`() = runTest {
        coEvery { api.downloadUrl(pickCode = "abc") } returns Response.success(
            OpenDownUrlResponse(
                state = true, code = 0, message = "",
                data = mapOf("fid_1" to downUrlItem("fid_1", "https://cdn.115.com/img.png")),
            )
        )

        val result = repo.fetchImageOriginalUrl("fid_1", "abc")

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.115.com/img.png", result.getOrThrow())
    }

    @Test
    fun `fetchImageOriginalUrl second call hits cache without invoking api`() = runTest {
        coEvery { api.downloadUrl(pickCode = "abc") } returns Response.success(
            OpenDownUrlResponse(
                state = true, code = 0, message = "",
                data = mapOf("fid_1" to downUrlItem("fid_1", "https://cdn.115.com/img.png")),
            )
        )

        // 第一次调用 → 走 api + 写缓存
        val first = repo.fetchImageOriginalUrl("fid_1", "abc")
        // 第二次同 fid 调用 → 应该命中缓存,**不**再调 api
        val second = repo.fetchImageOriginalUrl("fid_1", "abc")

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals("https://cdn.115.com/img.png", second.getOrThrow())
        coVerify(exactly = 1) { api.downloadUrl(pickCode = "abc") }
    }

    @Test
    fun `fetchImageOriginalUrl failure propagates without caching`() = runTest {
        coEvery { api.downloadUrl(pickCode = "bad") } returns Response.success(
            OpenDownUrlResponse(
                state = false, code = 990001, message = "提取码无效",
                data = emptyMap(),
            )
        )

        val result = repo.fetchImageOriginalUrl("fid_1", "bad")

        assertTrue(result.isFailure)
        assertEquals("提取码无效", result.exceptionOrNull()?.message)
        // 失败不写缓存
        coVerify(exactly = 1) { api.downloadUrl(pickCode = "bad") }
    }

    @Test
    fun `fetchImageOriginalUrl different fids do not share cache`() = runTest {
        coEvery { api.downloadUrl(pickCode = "pc1") } returns Response.success(
            OpenDownUrlResponse(
                state = true, code = 0, message = "",
                data = mapOf("fid_A" to downUrlItem("fid_A", "https://cdn.115.com/a.png")),
            )
        )
        coEvery { api.downloadUrl(pickCode = "pc2") } returns Response.success(
            OpenDownUrlResponse(
                state = true, code = 0, message = "",
                data = mapOf("fid_B" to downUrlItem("fid_B", "https://cdn.115.com/b.png")),
            )
        )

        val a = repo.fetchImageOriginalUrl("fid_A", "pc1")
        val b = repo.fetchImageOriginalUrl("fid_B", "pc2")

        assertEquals("https://cdn.115.com/a.png", a.getOrThrow())
        assertEquals("https://cdn.115.com/b.png", b.getOrThrow())
        coVerify(exactly = 1) { api.downloadUrl(pickCode = "pc1") }
        coVerify(exactly = 1) { api.downloadUrl(pickCode = "pc2") }
    }

    // ─────────────────── fetchAudioStreamUrl 缓存 ───────────────────

    @Test
    fun `fetchAudioStreamUrl second call hits cache`() = runTest {
        coEvery { api.downloadUrl(pickCode = "aud") } returns Response.success(
            OpenDownUrlResponse(
                state = true, code = 0, message = "",
                data = mapOf("fid_aud" to downUrlItem("fid_aud", "https://cdn.115.com/song.mp3")),
            )
        )

        repo.fetchAudioStreamUrl("fid_aud", "aud")
        repo.fetchAudioStreamUrl("fid_aud", "aud")

        coVerify(exactly = 1) { api.downloadUrl(pickCode = "aud") }
    }

    // ─────────────────── setStar (走限速) ───────────────────

    @Test
    fun `setStar success returns Unit`() = runTest {
        coEvery {
            api.updateFile(fileId = "fid_x", star = 1)
        } returns Response.success(OpenFileUpdateResponse(state = true, message = "", code = 0))

        val result = repo.setStar("fid_x", true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.updateFile(fileId = "fid_x", star = 1) }
    }

    @Test
    fun `setStar false sends star parameter 0`() = runTest {
        coEvery {
            api.updateFile(fileId = "fid_y", star = 0)
        } returns Response.success(OpenFileUpdateResponse(state = true, message = "", code = 0))

        repo.setStar("fid_y", false)

        coVerify(exactly = 1) { api.updateFile(fileId = "fid_y", star = 0) }
    }

    @Test
    fun `setStar failure propagates with 115 message`() = runTest {
        coEvery {
            api.updateFile(fileId = "fid_z", star = any())
        } returns Response.success(
            OpenFileUpdateResponse(state = false, message = "文件不存在", code = 990003)
        )

        val result = repo.setStar("fid_z", true)

        assertTrue(result.isFailure)
        assertEquals("文件不存在", result.exceptionOrNull()?.message)
    }

    private fun downUrlItem(fid: String, url: String) = OpenDownUrlItem(
        fileName = "",
        fileSize = 0L,
        pickCode = "",
        sha1 = "",
        url = OpenDownUrlUrl(url = url).also {
            // map 序列化时 key=fid;构造时也带上确保 mock 不依赖 key
            @Suppress("UnusedPrivateProperty") val _key = fid
        },
    )
}
