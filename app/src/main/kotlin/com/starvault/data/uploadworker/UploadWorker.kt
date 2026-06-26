package com.starvault.data.uploadworker

import android.app.Notification
import android.content.Context
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
import com.starvault.core.ServiceLocator
import com.starvault.core.UploadNotificationChannel
import com.starvault.data.upload.OssUploader
import com.starvault.data.upload.UploadInitClient
import java.util.UUID

/**
 * 115 上传 WorkManager 任务 — 把 [UploadExecutor] 包装成 CoroutineWorker。
 *
 * ## 责任分工
 *  - [UploadExecutor] : 业务编排(Init → 状态机 → Sign check → GetToken → OSS)— 纯 JVM 可测
 *  - [UploadWorker]   : WorkManager 适配器(setProgress / setForeground / URI→InputStream)— 需 Robolectric
 *
 * ## 进度上报
 *  - [ProgressKey.Transferred] (Long) : 已传输字节
 *  - [ProgressKey.Phase]      (String) : RUNNING / DONE / FAILED / CANCELED
 *
 * WorkManager `setProgress` 是 best-effort 通知,Transfers 屏 observe WorkInfo 拉取。
 *
 * ## ForegroundInfo
 *  - Phase 4 才有 `getForegroundInfo()` 真实实现(通知 + NotificationChannel)
 *  - 现在 stub 返 null,Phase 4 重写
 */
class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(Key.Uri) ?: return Result.failure()
        val targetCid = inputData.getString(Key.TargetCid) ?: return Result.failure()
        val fileName = inputData.getString(Key.FileName) ?: return Result.failure()
        val sizeBytes = inputData.getLong(Key.SizeBytes, -1L).takeIf { it > 0 } ?: return Result.failure()

        val uri = Uri.parse(uriStr)
        val resolver = applicationContext.contentResolver

        val executor: UploadExecutor = ServiceLocator.uploadExecutor
        return try {
            // 1. 切到 foreground(API 26+ 持久通知,扛 OEM 后台杀)
            setForeground(getForegroundInfo())
            // 2. 上报 RUNNING 阶段
            setProgress(workDataOf(
                ProgressKey.Phase to Phase.RUNNING,
                ProgressKey.Transferred to 0L,
                ProgressKey.TotalBytes to sizeBytes,
            ))

            val input = resolver.openInputStream(uri)
                ?: throw IllegalStateException("ContentResolver returned null for $uri")
            val result = executor.run(
                fileName = fileName,
                fileSize = sizeBytes,
                targetCid = targetCid,
                input = input,
                onProgress = { transferred, total ->
                    setProgress(
                        workDataOf(
                            ProgressKey.Phase to Phase.RUNNING,
                            ProgressKey.Transferred to transferred,
                            ProgressKey.TotalBytes to total,
                        )
                    )
                },
            )
            input.close()

            when (result) {
                is UploadOutcome.Success -> {
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.DONE,
                        ProgressKey.Transferred to sizeBytes,
                        ProgressKey.TotalBytes to sizeBytes,
                    ))
                    Result.success()
                }
                is UploadOutcome.Reject -> {
                    // ToastBus 已在 executor 里发过,这里只标 phase
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.FAILED,
                        ProgressKey.Transferred to 0L,
                        ProgressKey.TotalBytes to sizeBytes,
                    ))
                    Result.failure()
                }
                is UploadOutcome.Failure -> {
                    setProgress(workDataOf(
                        ProgressKey.Phase to Phase.FAILED,
                        ProgressKey.Transferred to 0L,
                        ProgressKey.TotalBytes to sizeBytes,
                    ))
                    Result.failure()
                }
            }
        } catch (t: Throwable) {
            // CancellationException = 协程被取消(进程被杀 / WorkManager stop / onCleared)
            // → 抛 Result.retry() 让 WorkManager 在 backoff 后自动重启(默认 EXPONENTIAL,30s 起步)
            // → 这是 M2 spec §6 进程崩溃后续传的关键
            if (t is kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "UploadWorker cancelled, retry: ${t.message}")
                setProgress(workDataOf(ProgressKey.Phase to Phase.CANCELED))
                return Result.retry()
            }
            Log.w(TAG, "UploadWorker.doWork failed", t)
            setProgress(workDataOf(ProgressKey.Phase to Phase.FAILED))
            Result.failure()
        }
    }

    /**
     * ForegroundInfo — 持续显示上传进度通知,API 34+ 必须 `FOREGROUND_SERVICE_TYPE_DATA_SYNC`。
     *
     * ## 通知内容
     *  - title: "正在上传 {fileName}"
     *  - text: "{transferred / 1 MB} / {total / 1 MB} ({percent}%)"  — Phase 4 简化版
     *  - setOngoing(true) 用户不能左划清掉
     *  - setProgress(100, percent, false) 显示横向进度条
     *
     * ## API 26+ 通知 channel
     *  - 每次进 foreground 前调 [UploadNotificationChannel.ensureCreated] 双保险
     *
     * ## API 34+ foreground service type
     *  - Android 14 强制要求 type 字段,否则 SecurityException
     *  - M2 spec §4: 走 `FOREGROUND_SERVICE_TYPE_DATA_SYNC`(115 上传是网络数据同步)
     *
     * ## 失败 / 完成
     *  - 失败时 WorkManager 会清掉 notification(自带 lifecycle),不需要主动 dismiss
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        UploadNotificationChannel.ensureCreated(applicationContext)
        val fileName = inputData.getString(Key.FileName) ?: "文件"
        val totalBytes = inputData.getLong(Key.SizeBytes, 0L)
        val transferred = inputData.getLong(ProgressKey.Transferred, 0L)
        val percent = if (totalBytes > 0) ((transferred * 100) / totalBytes).toInt() else 0
        val notification = buildNotification(fileName, transferred, totalBytes, percent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: 必须显式 setForegroundAsync(type)
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        fileName: String,
        transferred: Long,
        total: Long,
        percent: Int,
    ): Notification {
        val text = if (total > 0) {
            "${formatSize(transferred)} / ${formatSize(total)} ($percent%)"
        } else {
            "准备中…"
        }
        return NotificationCompat.Builder(applicationContext, UploadNotificationChannel.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("正在上传 $fileName")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, total == 0L)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "${bytes} B"
        val kb = bytes / 1024
        if (kb < 1024) return "${kb} KB"
        val mb = kb / 1024
        if (mb < 1024) return "${mb} MB"
        return "${bytes / (1024 * 1024 * 1024)} GB"
    }

    companion object {
        private const val TAG = "UploadWorker"

        /** ForegroundInfo 通知 id — 单上传任务用固定 1 个(Phase 3 限制 M2 只能 1 个并发任务)。 */
        const val NOTIFICATION_ID = 115_001

        /**
         * 构造 1 个 OneTimeWorkRequest(由 TransfersViewModel / UploadRoute 调用)。
         */
        fun enqueue(
            context: Context,
            uri: Uri,
            targetCid: String,
            fileName: String,
            sizeBytes: Long,
        ): UUID {
            val data: Data = workDataOf(
                Key.Uri to uri.toString(),
                Key.TargetCid to targetCid,
                Key.FileName to fileName,
                Key.SizeBytes to sizeBytes,
            )
            val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request.id
        }
    }

    /** WorkRequest input Data keys(顶层 const,方便测试 + 跨 module 调用)。 */
    object Key {
        const val Uri = "uri"
        const val TargetCid = "targetCid"
        const val FileName = "fileName"
        const val SizeBytes = "sizeBytes"
    }

    /** setProgress Data keys(顶层 const)。 */
    object ProgressKey {
        const val Phase = "phase"
        const val Transferred = "transferred"
        const val TotalBytes = "totalBytes"
    }

    /** Phase 字符串值(顶层 const,方便测试引用)。 */
    object Phase {
        const val RUNNING = "RUNNING"
        const val DONE = "DONE"
        const val FAILED = "FAILED"
        const val CANCELED = "CANCELED"
    }
}
