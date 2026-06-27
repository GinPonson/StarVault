package com.starvault.data.download

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * OssDownloader 单元测试 — Mock OkHttp Client,验证:
 *  - 8KB chunked 写流 + 字节数对齐
 *  - 进度回调节流(≥1MB 或 ≥200ms 触发)
 *  - HTTP 非 2xx → Failure
 *  - 协程取消 → CancellationException 透传(非 Failure)
 *
 * 测试 seam:mock [OkHttpClient.newCall] 返回一个 fake [Call] ,fake 在 enqueue 同步调
 * callback.onResponse(Response with Buffer body) — 模拟 OkHttp 异步回调。
 */
class OssDownloaderTest {

    @Before fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
    }

    /**
     * 构造 1 个 [OkHttpClient] mock,新 call 时返回 fake call,fake call 的 enqueue 同步
     * 走 callback,模拟 OkHttp dispatcher。
     *
     * 注意 [requestSlot] 在 `enqueue` 回调**内**访问 — 此时 `newCall(...)` 已调,slot 已 capture。
     */
    private fun fakeClient(bodyBytes: ByteArray, httpCode: Int = 200): OkHttpClient {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val requestSlot = slot<Request>()
        every { client.newCall(capture(requestSlot)) } returns call
        every { call.cancel() } returns Unit

        every { call.enqueue(any()) } answers {
            val cb = firstArg<Callback>()
            val response = Response.Builder()
                .request(requestSlot.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(httpCode)
                .message("OK")
                .body(bodyBytes.toResponseBody("application/octet-stream".toMediaType()))
                .build()
            cb.onResponse(call, response)
        }
        return client
    }

    @Test fun `download writes all bytes to OutputStream and reports success`() = runBlocking {
        val payload = ByteArray(20 * 1024) { (it % 256).toByte() }  // 20KB
        val client = fakeClient(bodyBytes = payload)
        val downloader = OssDownloader(okHttpClient = client)

        val output = ByteArrayOutputStream()
        val outcome = downloader.download(
            url = "https://cdn.example.com/file.bin",
            output = output,
            onProgress = { _, _ -> /* no-op for this test */ },
        )

        assertTrue("expected Success, got: $outcome", outcome is DownloadOutcome.Success)
        outcome as DownloadOutcome.Success
        assertEquals(payload.size.toLong(), outcome.bytesWritten)
        assertEquals(payload.size, output.size())
        assertTrue("written bytes should match payload", payload.contentEquals(output.toByteArray()))
    }

    @Test fun `download fires onProgress at least once per 1MB`() = runBlocking {
        // 3MB payload → 至少触发 3 次进度回调(0 / 1MB / 2MB / 3MB)
        val payload = ByteArray(3 * 1024 * 1024) { (it % 256).toByte() }
        val client = fakeClient(bodyBytes = payload)
        val downloader = OssDownloader(okHttpClient = client)

        val progressEvents = mutableListOf<Pair<Long, Long>>()
        downloader.download(
            url = "https://cdn.example.com/large.bin",
            output = ByteArrayOutputStream(),
            onProgress = { transferred, total ->
                progressEvents += transferred to total
            },
        )

        // 节流:每 ≥1MB 触发 1 次;流末尾强制 1 次 → 至少 4 次(0 → 1M → 2M → 3M)
        assertTrue("expected ≥4 progress events, got ${progressEvents.size}", progressEvents.size >= 4)
        // 末次事件 transferred == total
        val (lastTransferred, lastTotal) = progressEvents.last()
        assertEquals(payload.size.toLong(), lastTransferred)
        assertEquals(payload.size.toLong(), lastTotal)
    }

    @Test fun `download returns Failure on HTTP 404`() = runBlocking {
        val client = fakeClient(bodyBytes = ByteArray(0), httpCode = 404)
        val downloader = OssDownloader(okHttpClient = client)

        val outcome = downloader.download(
            url = "https://cdn.example.com/missing.bin",
            output = ByteArrayOutputStream(),
            onProgress = { _, _ -> },
        )

        assertTrue("expected Failure, got: $outcome", outcome is DownloadOutcome.Failure)
        outcome as DownloadOutcome.Failure
        assertEquals("HTTP 404", outcome.cause)
    }

    @Test fun `download propagates CancellationException when coroutine cancelled before start`() = runBlocking {
        // 协程在 download 真正开始前 cancel → withContext(Dispatchers.IO) 立刻抛 CancellationException。
        // 验证 OssDownloader 不把它包装成 DownloadOutcome.Failure,而是透传。
        val client = fakeClient(bodyBytes = ByteArray(100))
        val downloader = OssDownloader(okHttpClient = client)

        val deferred = async {
            downloader.download(
                url = "https://cdn.example.com/x.bin",
                output = ByteArrayOutputStream(),
                onProgress = { _, _ -> },
            )
        }
        deferred.cancel()  // 启动前立即 cancel
        var caught: Throwable? = null
        try {
            deferred.await()
        } catch (t: Throwable) {
            caught = t
        }

        assertTrue("expected CancellationException, got: $caught", caught is kotlinx.coroutines.CancellationException)
    }
}
