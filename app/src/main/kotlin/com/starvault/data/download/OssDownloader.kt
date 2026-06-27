package com.starvault.data.download

import android.util.Log
import com.starvault.core.ToastBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 115 CDN 流式下载 — OkHttp 拉签名 URL + 8KB chunked 写流 + 进度节流上报。
 *
 * ## 责任分工
 *  - [OssDownloader]         : 流式 GET + 写流 + 进度回调(纯 IO,可 JVM 单测)
 *  - [com.starvault.data.download.DownloadSaveUri] : MediaStore.Downloads insert + openOutputStream
 *  - [com.starvault.data.downloadworker.DownloadExecutor] : 编排 downurl → saveUri → download
 *
 * ## 取消语义
 *  - 协程被取消时 [suspendCancellableCoroutine] 的 `invokeOnCancellation` 触发 OkHttp `Call.cancel()`。
 *  - 取消后 input.read() 会抛 `IOException("Canceled")` —— 不是 `CancellationException`。
 *    在外层 catch 用 [ensureActive] 区分"协程已取消"(抛 CancellationException 透传,Worker 走 retry)
 *    vs "网络真出错"(返回 Failure,ToastBus 报错)。
 *
 * ## 进度节流(spec §6.2)
 *  - 每 1MB 或 200ms 触发一次 `onProgress`,避免 IPC 风暴。
 *  - 流末尾强制触发最后一次,保证进度 100% 落到 UI。
 *
 * ## 已知风险(plan #11)
 *  - 复用 [ServiceLocator.okHttpClient],带 `Token401Interceptor`。签名 URL 5 分钟过期时
 *    401 触发 refresh + retry,会浪费 ~60s 才 fail。暂接受,M4 优化(独立 downloadOkHttpClient)。
 */
class OssDownloader(
    private val okHttpClient: OkHttpClient,
) {

    /**
     * 流式 GET [url] 写入 [output]。
     *
     * @param output 调用方提供的输出流(由 [com.starvault.data.download.DownloadSaveUri]
     *               通过 `ContentResolver.openOutputStream` 拿到,OssDownloader 内部
     *               会套一层 BufferedOutputStream,调用方无需预先 buffer)。
     * @param onProgress suspend 回调 — [com.starvault.data.downloadworker.DownloadWorker]
     *                   据此调 `setProgress(workDataOf(Transferred, TotalBytes))`。
     */
    suspend fun download(
        url: String,
        output: OutputStream,
        onProgress: suspend (transferredBytes: Long, totalBytes: Long) -> Unit,
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val call = okHttpClient.newCall(request)
        val bufferedOutput = output.buffered()

        try {
            val response = call.awaitResponse()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                val msg = "HTTP $code"
                Log.w(TAG, "download failed: $msg for $url")
                ToastBus.error("下载失败:$msg")
                return@withContext DownloadOutcome.Failure(msg)
            }

            val body = response.body
                ?: run {
                    response.close()
                    val msg = "empty body"
                    Log.w(TAG, "download failed: $msg")
                    ToastBus.error("下载失败:$msg")
                    return@withContext DownloadOutcome.Failure(msg)
                }

            val total = body.contentLength().takeIf { it > 0 } ?: 0L
            val input = body.byteStream().buffered()

            var transferred = 0L
            var lastReported = 0L
            var lastReportAtMs = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            try {
                while (true) {
                    // 循环开头先校验 active — 如果协程已取消,主动抛 CancellationException
                    // 让外层 catch 知道是"取消"而非"IO 错误",Worker 会走 Result.retry()。
                    currentCoroutineContext().ensureActive()

                    val read = input.read(buffer)
                    if (read == -1) break
                    bufferedOutput.write(buffer, 0, read)
                    transferred += read

                    val now = System.currentTimeMillis()
                    val bytesDelta = transferred - lastReported
                    val timeDelta = now - lastReportAtMs
                    if (bytesDelta >= PROGRESS_BYTES || timeDelta >= PROGRESS_INTERVAL_MS) {
                        onProgress(transferred, total)
                        lastReported = transferred
                        lastReportAtMs = now
                    }
                }
                bufferedOutput.flush()

                // 流末尾强制最后 1 次上报,确保进度到 100%(Worker 据此 setProgress DONE)
                onProgress(transferred, total)

                DownloadOutcome.Success(bytesWritten = transferred, totalBytes = total)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                // OkHttp call.cancel() 触发后 read() 抛 IOException("Canceled") —
                // 此时协程已 inactive,转抛 CancellationException 让 Worker 走 retry。
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "download failed mid-stream", t)
                ToastBus.error(t.message ?: "下载失败")
                DownloadOutcome.Failure(t.message ?: "下载失败")
            } finally {
                try { input.close() } catch (_: Throwable) {}
                response.close()
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "download failed", t)
            ToastBus.error(t.message ?: "下载失败")
            DownloadOutcome.Failure(t.message ?: "下载失败")
        }
    }

    /**
     * 把 OkHttp 异步 enqueue 包成 suspend。
     *
     * - 协程取消时 [invokeOnCancellation] 触发 `Call.cancel()`,OkHttp 会断开 socket。
     * - 已 enqueue 但还没回调时协程被取消 → onResponse 还会触发,但 `cont.isCancelled` 为 true,
     *   此时关闭 response 释放连接。
     */
    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            try { cancel() } catch (_: Throwable) { /* already done — ignore */ }
        }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isCancelled) return
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isCancelled) {
                    response.close()
                    return
                }
                cont.resume(response)
            }
        })
    }

    private companion object {
        const val TAG = "OssDownloader"

        /** 每次 read/write buffer 大小 — 8KB 是 SSD/磁盘 + TCP socket 的常见甜点。 */
        const val BUFFER_SIZE = 8 * 1024

        /** 进度上报阈值:累计 ≥1MB 才上报,避免每 chunk 8KB 触发 IPC。 */
        const val PROGRESS_BYTES = 1L * 1024 * 1024

        /** 进度上报间隔:≥200ms 才上报,作为"长时间没攒够 1MB"的兜底。 */
        const val PROGRESS_INTERVAL_MS = 200L
    }
}

/**
 * OssDownloader 执行结果。
 *
 * - [Success] : 写流成功完成,字节数对齐
 * - [Failure] : HTTP 非 2xx / IO 异常 / 流被截断
 *
 * 不区分 Reject(下载场景下没有"业务秒传"这种拒绝语义)。
 * 取消走 [CancellationException] 透传,不会到此处。
 */
sealed interface DownloadOutcome {
    data class Success(
        val bytesWritten: Long,
        val totalBytes: Long,
    ) : DownloadOutcome

    data class Failure(
        val cause: String,
    ) : DownloadOutcome
}
