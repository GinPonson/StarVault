package com.starvault.data.repository

import android.content.Context
import com.starvault.data.downloadworker.DownloadWork
import com.starvault.data.downloadworker.DownloadWorker
import com.starvault.data.remote.cloud115.ParsedFileItem
import kotlinx.coroutines.channels.Channel
import java.util.UUID

/**
 * 115 单文件下载入口仓库(M3)— 把 [ParsedFileItem] 转成 WorkRequest,投递到 WorkManager,
 * 并把 workId 桥接到 [com.starvault.ui.transfers.TransfersViewModel.observeDownloadWork]。
 *
 * ## 责任分工
 *  - [DownloadRepository]      : 校验 + WorkRequest 构造 + 跨 VM 桥接(纯逻辑,可 JVM 单测)
 *  - [DownloadWorker]          : WorkManager 适配(setProgress / setForeground / ForegroundInfo)
 *  - [com.starvault.data.downloadworker.DownloadExecutor] : 业务编排(downurl → 写流)
 *
 * ## 跨 VM 桥接
 *  `enqueue` 成功后 `downloadWorkTrigger.trySend(DownloadWork(workId, fileName, sizeBytes))` —
 *  [com.starvault.ui.transfers.TransfersViewModel] 在 appScope 内 collect 这个 Channel,
 *  收到 [DownloadWork] 后调 [observeDownloadWork] 订阅 WorkInfo 进度(对齐 M2
 *  `filesRefreshTrigger` 模式)。envelope 多带 fileName/sizeBytes 是因为
 *  WorkManager 2.10.3 WorkInfo 不暴露 inputData,VM 拿不到展示元数据。
 *
 * ## 校验
 *  - [ParsedFileItem.isFolder] == true → [Result.failure]("文件夹不支持下载")
 *  - [ParsedFileItem.pickCode].isBlank() → [Result.failure]("缺少提取码")
 *  - 校验失败**不**投递 WorkRequest(Worker 拿到空 pickCode 会 crash)
 *
 *  校验同时在 [com.starvault.ui.files.FilesViewModel.downloadEntry] 做(UX 弹 ToastBus.error),
 *  本处是 defense-in-depth。
 */
class DownloadRepository(
    private val context: Context,
    private val downloadWorkTrigger: Channel<DownloadWork>,
) {

    /**
     * 投递 1 个文件下载任务。
     *
     * @param item 待下载的文件(非文件夹,且 pickCode 非空)
     * @return 成功 → [Result.success] 含 [UUID](调用方可挂到自己的 UI 状态);
     *         失败 → [Result.failure] 含错误原因(已写明,UI 可直接展示)
     */
    fun enqueue(item: ParsedFileItem): Result<UUID> {
        if (item.isFolder) {
            return Result.failure(IllegalStateException("文件夹不支持下载"))
        }
        if (item.pickCode.isBlank()) {
            return Result.failure(IllegalStateException("缺少提取码"))
        }
        val workId = DownloadWorker.enqueue(
            context = context,
            pickCode = item.pickCode,
            fileId = item.id,
            fileName = item.name,
            // M3:saveName 与 fileName 同(系统 MediaStore 自动处理重名);
            // M4 批量下载时再考虑 `_${index}` 之类去重后缀。
            saveName = item.name,
            sizeBytes = item.sizeBytes,
        )
        // 桥接到 TransfersViewModel.observeDownloadWork(进程级 appScope 收集)。
        // envelope 携带 fileName + sizeBytes — WorkManager 2.10.3 WorkInfo 不暴露
        // inputData,VM 侧拿到 envelope 后直接建占位 entry,不用再回去查 source。
        downloadWorkTrigger.trySend(
            DownloadWork(
                workId = workId,
                fileName = item.name,
                sizeBytes = item.sizeBytes,
            ),
        )
        return Result.success(workId)
    }
}
