package com.starvault.data.uploadworker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.starvault.core.ServiceLocator
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
            // 上报 RUNNING 阶段
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
            Log.w(TAG, "UploadWorker.doWork failed", t)
            setProgress(workDataOf(ProgressKey.Phase to Phase.FAILED))
            Result.failure()
        }
    }

    /**
     * ForegroundInfo — Phase 4 真实实现(NotificationChannel + progress notification)。
     *
     * 现在 stub:Phase 4 替换为:
     *  - `NotificationCompat.Builder(ctx, CHANNEL_ID)`
     *  - `setOngoing(true)` + `setProgress(100, percent, false)`
     *  - `ForegroundInfo(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`
     */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        // Phase 4 替换
        @Suppress("DEPRECATION")
        ForegroundInfo(
            /* notificationId = */ 0,
            /* notification = */ android.app.Notification(),
        )

    companion object {
        private const val TAG = "UploadWorker"

        /** WorkRequest input Data keys。 */
        object Key {
            const val Uri = "uri"
            const val TargetCid = "targetCid"
            const val FileName = "fileName"
            const val SizeBytes = "sizeBytes"
        }

        /** setProgress Data keys。 */
        object ProgressKey {
            const val Phase = "phase"
            const val Transferred = "transferred"
            const val TotalBytes = "totalBytes"
        }

        /** Phase 字符串值。 */
        object Phase {
            const val RUNNING = "RUNNING"
            const val DONE = "DONE"
            const val FAILED = "FAILED"
            const val CANCELED = "CANCELED"
        }

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
}
