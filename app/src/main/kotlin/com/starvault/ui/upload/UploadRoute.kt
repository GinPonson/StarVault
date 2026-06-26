package com.starvault.ui.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.starvault.data.uploadworker.UploadWorker
import com.starvault.ui.transfers.TransfersViewModel
import java.util.UUID

/**
 * 115 上传 UI 入口 — Composable launcher,挂到 FilesRoute.onUpload。
 *
 * ## 用法
 * ```
 * UploadLauncher(
 *     targetCid = currentFolderCid,  // 从 FilesUiState.Success.folderId
 *     onUploaded = { workId -> transfersViewModel.observeWork(workId, fileName, sizeBytes) },
 * )
 *
 * // 然后用 `rememberUploadLauncher()` 拿到 launcher,放进 onClick:
 * val launch = rememberUploadLauncher()
 * Button(onClick = { launch() }) { Text("Upload") }
 * ```
 *
 * ## SAF ([ActivityResultContracts.OpenDocument])
 *  - **不能用 GetContent**:GetContent 返回的 URI 是 process-scoped grant,进程被杀后
 *    WorkManager 在新进程重启 [UploadWorker] 时 `ContentResolver.openInputStream` 会
 *    `SecurityException`(M2 spec §6 续传路径最大坑)
 *  - OpenDocument 返回的 URI 带 `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`,
 *    picker 回调里立刻 [ContentResolver.takePersistableUriPermission] 锁定 grant,
 *    grant 持久化到 ContentResolver,跨进程仍可读
 *  - 走 DocumentsUI(支持 folder navigation),不是 GetContent 的简化单选面板
 *
 * ## 转账
 *  - picker 拿到 Uri → takePersistableUriPermission → queryFileMeta 拿 displayName + size
 *  - `UploadWorker.enqueue(ctx, uri, targetCid, displayName, sizeBytes)` 返回 UUID
 *  - `transfersViewModel.observeWork(uuid, displayName, sizeBytes)` 启动 collect
 */
@Composable
fun rememberUploadLauncher(
    onResult: (UploadFileMeta) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // 关键:OpenDocument 默认带 FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        // 这里 take 之后 grant 持久化,即使进程被杀 + WorkManager 在新进程重启 Worker,
        // ContentResolver.openInputStream 仍能读(GetContent contract 不支持,必抛 SecurityException)
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // 极少数 picker 不带 persistable flag(罕见,如某些 OEM gallery picker),
            // 当前 picker 选完用户已看到文件选择完成;继续走,后续 force-stop 重启时
            // 会落到旧路径的 SecurityException(已知问题,M3+ 切 copy-to-cache 兜底)
            Log.w(TAG, "takePersistableUriPermission failed for $uri", e)
        }
        val meta = queryFileMeta(context.contentResolver, uri)
        onResult(meta)
    }
    return remember(launcher) { { launcher.launch(arrayOf("*/*")) } }
}

/**
 * 便捷入口:接收 fileMeta + targetCid + Trans VM,enqueue work + observe progress。
 *
 * - 用 `LaunchedEffect(fileMeta)` 触发 enqueue(只触发 1 次,避免 picker 重入)
 * - 不阻塞 UI;enqueue 后即返回,worker 在后台跑
 */
@Composable
fun UploadLaunchAndEnqueue(
    fileMeta: UploadFileMeta?,
    targetCid: String,
    transfersViewModel: TransfersViewModel,
) {
    val context = LocalContext.current
    LaunchedEffect(fileMeta) {
        if (fileMeta == null) return@LaunchedEffect
        val workId: UUID = UploadWorker.enqueue(
            context = context,
            uri = fileMeta.uri,
            targetCid = targetCid,
            fileName = fileMeta.displayName,
            sizeBytes = fileMeta.sizeBytes,
        )
        transfersViewModel.observeWork(
            workId = workId,
            fileName = fileMeta.displayName,
            totalBytes = fileMeta.sizeBytes,
        )
    }
}

/**
 * 顶层 helper:一次性把 picker + enqueue + observeWork 串起来,给 FilesRoute 1 行调。
 *
 * @return Unit — 调用方在 onUpload lambda 里 invoke 即可
 */
fun enqueueUpload(
    context: Context,
    uri: Uri,
    targetCid: String,
    transfersViewModel: TransfersViewModel,
) {
    val meta = queryFileMeta(context.contentResolver, uri)
    val workId = UploadWorker.enqueue(
        context = context,
        uri = meta.uri,
        targetCid = targetCid,
        fileName = meta.displayName,
        sizeBytes = meta.sizeBytes,
    )
    transfersViewModel.observeWork(
        workId = workId,
        fileName = meta.displayName,
        totalBytes = meta.sizeBytes,
    )
}

private const val TAG = "UploadRoute"
