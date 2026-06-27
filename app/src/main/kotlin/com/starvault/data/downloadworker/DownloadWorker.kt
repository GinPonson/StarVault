package com.starvault.data.downloadworker

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.starvault.core.DownloadNotificationChannel
import com.starvault.core.ServiceLocator
import com.starvault.data.download.DownloadSaveUri
import java.util.UUID

/**
 * 115 下载 WorkManager 任务 — 把 [DownloadExecutor] 包装成 CoroutineWorker。
 *
 * ## 责任分工
 *  - [DownloadExecutor] : 业务编排(downurl → MediaStore.insert → 流式写 → publish/delete)
 *  - [DownloadWorker]   : WorkManager 适配器(setProgress / setForeground / ForegroundInfo 构造)
 *
 * ## 进度上报
 *  - [ProgressKey.Transferred] (Long) : 已下载字节
 *  - [ProgressKey.Phase]      (String) : RUNNING / DONE / FAILED / CANCELED
 *
 * WorkManager `setProgress` 是 best-effort 通知,Transfers 屏 observe WorkInfo 拉取。
 *
 * ## ForegroundInfo / 通知栏状态机
 *
 * - **多并发**:NOTIFICATION_ID = `id.hashCode()`(每个 work 独立,允许多文件同时下载)。
 * - 进度阶段:`setForeground(buildProgressInfo(percent))` — `stat_sys_download` 图标 + 横向进度条,
 *   无 PendingIntent(downloading 时 destUri 尚未生成)。
 * - 完成阶段:`postCompletionNotification(fileName, targetUri, mime)` — `stat_sys_download_done`
 *   图标 + `Intent.ACTION_VIEW + destUri` 的 PendingIntent(API 31+ `FLAG_IMMUTABLE` 必填)。
 *   通过 [NotificationManager.notify] 直接 post 到**独立 ID** [COMPLETION_NOTIFICATION_TAG],**绕开
 *   WorkManager 的 FGS 通知生命周期** — 因为 [Result.success] 返回后 WM 会调
 *   `SystemForegroundDispatcher.cancelNotification` 移除同 id 通知,完成态必须脱离 WM 管控
 *   才能保留在通知栏。用户点 → 系统文件管理器打开 destUri。
 * - 失败阶段:`setForeground(buildFailureInfo(fileName))` — `stat_notify_error` 图标,无 action
 *   (失败走 ToastBus 已提示)。Result.failure() 后 FGS 通知被 WM 移除,但失败已由 ToastBus 提示,
 *   不需要残留通知。
 *
 * ## 取消 / 重试
 *  - 协程被取消 → [DownloadExecutor] 抛 [kotlinx.coroutines.CancellationException] →
 *    catch 后 setProgress(CANCELED) + `Result.retry()`(WorkManager backoff 后自动重启,
 *    新一次重试拿新 5 分钟签名 URL)。
 *  - 其他异常 → setProgress(FAILED) + `Result.failure()` + post 失败通知。
 *
 * ## 不做 runAttemptCount cap
 *  WorkManager 默认 EXPONENTIAL backoff(30s 起步),5 次后放弃。每次 retry 重新走 downurl,
 *  拿新签名 URL(plan 风险 #3)。
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pickCode = inputData.getString(Key.PickCode) ?: return Result.failure()
        val fileId = inputData.getString(Key.FileId)  // 可空:fallback 取 data 首项
        val fileName = inputData.getString(Key.FileName) ?: return Result.failure()
        val saveName = inputData.getString(Key.SaveName) ?: fileName  // 默认 = fileName
        val sizeBytes = inputData.getLong(Key.SizeBytes, -1L).takeIf { it >= 0 } ?: return Result.failure()

        val executor: DownloadExecutor = ServiceLocator.downloadExecutor

        return try {
            // 1. 切到 foreground(API 26+ 持久通知,扛 OEM 后台杀)
            setForeground(buildProgressInfo(fileName, transferred = 0L, total = sizeBytes))
            // 2. 上报 RUNNING 阶段
            setProgress(workDataOf(
                ProgressKey.Phase to Phase.RUNNING,
                ProgressKey.Transferred to 0L,
                ProgressKey.TotalBytes to sizeBytes,
            ))

            // 3. 调 executor(内部走 downurl → insert → 流式写)
            val result = executor.run(
                pickCode = pickCode,
                fileId = fileId,
                fileName = saveName,
                onProgress = { transferred, total ->
                    val effectiveTotal = if (total > 0) total else sizeBytes
                    val percent = if (effectiveTotal > 0) ((transferred * 100) / effectiveTotal).toInt() else 0
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.RUNNING,
                        ProgressKey.Transferred to transferred,
                        ProgressKey.TotalBytes to effectiveTotal,
                    ))
                    // 通知栏跟随进度更新(同 id 就地替换)
                    setForeground(buildProgressInfo(fileName, transferred, effectiveTotal))
                },
            )

            when (result) {
                is DownloadResult.Success -> {
                    val mime = DownloadSaveUri.mimeTypeFromName(saveName)
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.DONE,
                        ProgressKey.Transferred to sizeBytes,
                        ProgressKey.TotalBytes to sizeBytes,
                    ))
                    postCompletionNotification(
                        fileName = saveName,
                        targetUri = result.targetUri,
                        mime = mime,
                    )
                    Result.success()
                }
                is DownloadResult.Failure -> {
                    // ToastBus 已在 executor 里发过
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.FAILED,
                        ProgressKey.Transferred to 0L,
                        ProgressKey.TotalBytes to sizeBytes,
                    ))
                    setForeground(buildFailureInfo(fileName = saveName))
                    Result.failure()
                }
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) {
                // 进程被杀 / WorkManager stop / onCleared → 让 WorkManager backoff 后重启,
                // 拿新 5 分钟签名 URL 重下整文件(spec §6.2 + plan 风险 #1)
                Log.w(TAG, "DownloadWorker cancelled, will retry: ${t.message}")
                setProgress(workDataOf(ProgressKey.Phase to Phase.CANCELED))
                return Result.retry()
            }
            Log.w(TAG, "DownloadWorker.doWork failed", t)
            setProgress(workDataOf(ProgressKey.Phase to Phase.FAILED))
            setForeground(buildFailureInfo(fileName = saveName))
            Result.failure()
        }
    }

    /**
     * 进度阶段 ForegroundInfo — `stat_sys_download` 图标 + 横向进度条,无 PendingIntent。
     */
    private fun buildProgressInfo(fileName: String, transferred: Long, total: Long): ForegroundInfo {
        DownloadNotificationChannel.ensureCreated(applicationContext)
        val percent = if (total > 0) ((transferred * 100) / total).toInt() else 0
        val text = if (total > 0) {
            "${formatSize(transferred)} / ${formatSize(total)} ($percent%)"
        } else {
            "准备中…"
        }
        return wrapForegroundInfo(
            NotificationCompat.Builder(applicationContext, DownloadNotificationChannel.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("正在下载 $fileName")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, percent, total == 0L)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    /**
     * 完成阶段通知 — `stat_sys_download_done` 图标 + `Intent.ACTION_VIEW` PendingIntent。
     *
     * 通过 [NotificationManager.notify] 直接 post,**不使用** [setForeground] — 后者会被
     * WorkManager 在 [Result.success] 后被 `SystemForegroundDispatcher.cancelNotification` 移除。
     * 这里用独立 tag 维度(id 部分 + tag 部分)发,WM 只取消它自己登记的 id,我们的保留在通知栏,
     * 用户点 → 系统文件管理器打开 destUri。
     */
    private fun postCompletionNotification(fileName: String, targetUri: Uri, mime: String) {
        DownloadNotificationChannel.ensureCreated(applicationContext)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            /* requestCode = */ notificationId(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, DownloadNotificationChannel.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("下载完成")
            .setContentText(fileName)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(COMPLETION_NOTIFICATION_TAG, notificationId(), notification)
    }

    /**
     * 失败阶段 ForegroundInfo — `stat_notify_error` 图标,无 action(失败走 ToastBus 已提示)。
     */
    private fun buildFailureInfo(fileName: String): ForegroundInfo {
        DownloadNotificationChannel.ensureCreated(applicationContext)
        return wrapForegroundInfo(
            NotificationCompat.Builder(applicationContext, DownloadNotificationChannel.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("下载失败")
                .setContentText(fileName)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    /**
     * 包成 [ForegroundInfo] — API 34+ 必须显式 `FOREGROUND_SERVICE_TYPE_DATA_SYNC`(沿用
     * M2 上传的 DATA_SYNC type,下载也属网络数据同步)。
     */
    private fun wrapForegroundInfo(notification: Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notificationId(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId(), notification)
        }

    /** NOTIFICATION_ID = `id.hashCode()` — 多并发(plan 用户决策),每个 work 独立 id。 */
    private fun notificationId(): Int = id.hashCode()

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "${bytes} B"
        val kb = bytes / 1024
        if (kb < 1024) return "${kb} KB"
        val mb = kb / 1024
        if (mb < 1024) return "${mb} MB"
        return "${bytes / (1024 * 1024 * 1024)} GB"
    }

    companion object {
        private const val TAG = "DownloadWorker"

        /**
         * 完成通知的 tag 维度 — 与 FGS 通知的 id 区分,绕开 WorkManager cancelNotification。
         * tag + id 是 [NotificationManager.notify] 的复合 key;tag 唯一即可,id 仍按
         * `notificationId()`(每个 work 独立)排布,允许多文件同时完成。
         */
        private const val COMPLETION_NOTIFICATION_TAG = "download_complete"

        /**
         * 构造 1 个 OneTimeWorkRequest(由 [com.starvault.data.repository.DownloadRepository] 调用)。
         *
         * @param saveName 保存到 Downloads 的显示名(M3 与 fileName 同值,留字段供 M4 自定义)
         */
        fun enqueue(
            context: Context,
            pickCode: String,
            fileId: String?,
            fileName: String,
            saveName: String = fileName,
            sizeBytes: Long,
        ): UUID {
            val data: Data = if (fileId != null) {
                workDataOf(
                    Key.PickCode to pickCode,
                    Key.FileId to fileId,
                    Key.FileName to fileName,
                    Key.SaveName to saveName,
                    Key.SizeBytes to sizeBytes,
                )
            } else {
                workDataOf(
                    Key.PickCode to pickCode,
                    Key.FileName to fileName,
                    Key.SaveName to saveName,
                    Key.SizeBytes to sizeBytes,
                )
            }
            val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request.id
        }

        /** WorkRequest input Data keys(顶层 const,方便测试 + 跨 module 调用)。 */
        object Key {
            const val PickCode = "pickCode"
            const val FileId = "fileId"
            const val FileName = "fileName"
            const val SaveName = "saveName"
            const val SizeBytes = "sizeBytes"
        }
    }

    /** setProgress Data keys(顶层 const)。 */
    object ProgressKey {
        const val Phase = "phase"
        const val Transferred = "transferred"
        const val TotalBytes = "totalBytes"
    }

    /** Phase 字符串值(顶层 const,方便测试引用 + TransfersViewModel observeDownloadWork 用)。 */
    object Phase {
        const val RUNNING = "RUNNING"
        const val DONE = "DONE"
        const val FAILED = "FAILED"
        const val CANCELED = "CANCELED"
    }
}
