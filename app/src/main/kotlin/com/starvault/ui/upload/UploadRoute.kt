package com.starvault.ui.upload

import android.content.Context
import android.net.Uri
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
 * ## SAF
 *  - `ActivityResultContracts.GetContent('* / *')` 走系统 picker,不需要 READ_EXTERNAL_STORAGE
 *  - `Uri` 是 `content://` 形式,交给 [UploadWorker] 用 ContentResolver.openInputStream 读
 *
 * ## 转账
 *  - picker 拿到 Uri → queryFileMeta 拿 displayName + size
 *  - `UploadWorker.enqueue(ctx, uri, targetCid, displayName, sizeBytes)` 返回 UUID
 *  - `transfersViewModel.observeWork(uuid, displayName, sizeBytes)` 启动 collect
 */
@Composable
fun rememberUploadLauncher(
    onResult: (UploadFileMeta) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val meta = queryFileMeta(context.contentResolver, uri)
        onResult(meta)
    }
    return remember(launcher) { { launcher.launch("*/*") } }
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
