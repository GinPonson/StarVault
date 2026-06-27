package com.starvault.data.downloadworker

import android.net.Uri
import android.util.Log
import com.starvault.core.ToastBus
import com.starvault.data.download.DownloadOutcome
import com.starvault.data.download.DownloadSaveUri
import com.starvault.data.download.OssDownloader
import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.requireSuccessful
import kotlinx.coroutines.CancellationException

/**
 * 115 下载业务流程编排 — downurl → MediaStore.insert → 流式写 → publish/delete。
 *
 * ## 为什么拆出这个类
 *
 * [com.starvault.data.downloadworker.DownloadWorker] 是 WorkManager 适配器,负责:
 *  - setProgress / setForeground / NotificationChannel ensureCreated
 *  - 把 executor 返回的 [DownloadResult] 映射成 `Result.success/failure/retry`
 *
 * Worker 跑在 WorkManager runtime 上,单测需要 Robolectric。把核心编排(纯 IO + 业务决策)
 * 抽到本类后,核心 3 个 case(downurl 成功 / 失败 / 取消)在纯 JVM 就能 TDD。
 *
 * ## 进度回调契约
 *
 * [onProgress] 由 Worker 注入,内部透传给 [OssDownloader.download]。每 1MB / 200ms 触发 1 次
 * (节流由 OssDownloader 内部完成),Worker 据此 `setProgress(workDataOf("transferred" to N))`。
 *
 * ## 取消语义
 *
 * - 协程被取消时 [OssDownloader] 内部 throw [CancellationException] 透传,本类 catch 后:
 *   1. delete MediaStore IS_PENDING=1 行(不留 0 字节 ghost)
 *   2. 重新 throw CancellationException(Worker 据此走 Result.retry,新一次重试拿新签名 URL)
 * - 调用 [downloadSaveUri.prepare] 时 cancel 不影响(非 suspend)
 *
 * ## 不调 transferRepo
 *
 * 镜像 M2 [com.starvault.data.uploadworker.UploadExecutor]:Transfer entry 由
 * [com.starvault.ui.transfers.TransfersViewModel.observeDownloadWork] 在首个 RUNNING
 * setProgress 时创建,完成态由 markDone/markFailed。Executor 自身纯 IO,无 UI 状态。
 */
class DownloadExecutor(
    private val api: OpenFileApiService,
    private val downloadSaveUri: DownloadSaveUri,
    private val ossDownloader: OssDownloader,
) {

    /**
     * 跑完整下载流程(纯 IO + 决策,无 WorkManager 依赖)。
     *
     * @param pickCode 文件提取码(必填,从 [com.starvault.data.remote.cloud115.ParsedFileItem] 拿)
     * @param fileId 文件 ID(可选)— 有就直接 `data[fileId]` 拿直链,无则 fallback 第一项
     * @param fileName 保存到 Downloads 的显示名(用原文件名,系统自动处理重名)
     * @param onProgress suspend 回调 — 透传给 OssDownloader,1MB / 200ms 节流
     *
     * @return [DownloadResult.Success] 包含 MediaStore Uri(供通知栏 ACTION_VIEW);
     *         [DownloadResult.Failure] 含错误描述(Worker 走 Result.failure + 已发 toast)
     */
    suspend fun run(
        pickCode: String,
        fileId: String?,
        fileName: String,
        onProgress: suspend (transferredBytes: Long, totalBytes: Long) -> Unit,
    ): DownloadResult {
        // 1. 拿签名 CDN URL
        val cdnUrl = try {
            fetchCdnUrl(pickCode = pickCode, fileId = fileId)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "fetchCdnUrl failed", t)
            ToastBus.error(t.message ?: "取直链失败")
            return DownloadResult.Failure(t.message ?: "取直链失败")
        }

        // 2. MediaStore insert(IS_PENDING=1) + openOutputStream
        val mime = DownloadSaveUri.mimeTypeFromName(fileName)
        val targetUri = downloadSaveUri.prepare(fileName = fileName, mimeType = mime)
            ?: run {
                val msg = "无法创建下载目录条目"
                Log.w(TAG, msg)
                ToastBus.error(msg)
                return DownloadResult.Failure(msg)
            }

        val output = downloadSaveUri.openOutputStream(targetUri)
        if (output == null) {
            downloadSaveUri.delete(targetUri)
            val msg = "无法打开下载输出流"
            Log.w(TAG, msg)
            ToastBus.error(msg)
            return DownloadResult.Failure(msg)
        }

        // 3. 流式下载 + write + publish/delete on outcome
        return try {
            when (val outcome = ossDownloader.download(cdnUrl, output, onProgress)) {
                is DownloadOutcome.Success -> {
                    downloadSaveUri.publish(targetUri)
                    Log.i(TAG, "download success: $fileName → $targetUri (${outcome.bytesWritten} bytes)")
                    DownloadResult.Success(targetUri)
                }
                is DownloadOutcome.Failure -> {
                    downloadSaveUri.delete(targetUri)
                    Log.w(TAG, "download failed: ${outcome.cause}")
                    DownloadResult.Failure(outcome.cause)
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                // 协程被取消 — 清 ghost 行,重新 throw 让 Worker 走 retry
                downloadSaveUri.delete(targetUri)
                Log.w(TAG, "download cancelled, ghost row cleaned")
                throw t
            }
            Log.w(TAG, "download unexpected error", t)
            ToastBus.error(t.message ?: "下载失败")
            downloadSaveUri.delete(targetUri)
            DownloadResult.Failure(t.message ?: "下载失败")
        } finally {
            try { output.close() } catch (_: Throwable) { /* ignore */ }
        }
    }

    /**
     * 调 downurl 拿签名 CDN URL。
     *
     * 错误处理:state=false 或 HTTP 非 2xx 时抛 [IllegalStateException],外层 catch 转
     * [DownloadResult.Failure] + ToastBus。
     */
    private suspend fun fetchCdnUrl(pickCode: String, fileId: String?): String {
        val body = api.downloadUrl(pickCode).requireSuccessful()
        if (body.state != true) {
            throw IllegalStateException(body.message ?: "downurl 失败")
        }
        val item = fileId?.let { body.data[it] }
            ?: body.data.values.firstOrNull()
            ?: throw IllegalStateException("downurl 响应为空")
        val url = item.url.url
        if (url.isBlank()) throw IllegalStateException("downurl URL 为空")
        return url
    }

    private companion object {
        const val TAG = "DownloadExecutor"
    }
}

/**
 * Executor 运行结果 — 映射到 [com.starvault.data.downloadworker.DownloadWorker]
 * 的 `Result.success/failure/retry`。
 *
 * - [Success] : 写流完成 + IS_PENDING=0,包含 targetUri(通知栏 ACTION_VIEW 用)
 * - [Failure] : 取直链失败 / HTTP 错误 / 流被截断(已发 toast,Worker 只 Result.failure)
 *
 * 取消不进入此处 — CancellationException 由外层 catch 处理(走 retry)。
 */
sealed interface DownloadResult {
    data class Success(val targetUri: Uri) : DownloadResult
    data class Failure(val cause: String) : DownloadResult
}
