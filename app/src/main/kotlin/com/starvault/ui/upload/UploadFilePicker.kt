package com.starvault.ui.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * 115 上传文件元数据 — 来自 SAF / ContentResolver 解析。
 *
 * @param sizeBytes 0 表示未知(部分 provider 不报 SIZE)— UploadWorker 应该 fail-fast
 */
data class UploadFileMeta(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
)

/**
 * 给 [ContentResolver] 调,提取 [UploadFileMeta]。
 *
 * 关键字段:
 *  - [OpenableColumns.DISPLAY_NAME] — 文件名(给 Transfer row / 通知)
 *  - [OpenableColumns.SIZE]        — 字节数(给 transfer.totalBytes)
 *
 * 不调 `ContentResolver.getType(...)` — 我们传任何类型(M2 限定 M2 上传不限文件类型)。
 *
 * 用法:在 [UploadRoute] 调 `ActivityResultContracts.GetContent` 拿到 `Uri` 后,
 * 立刻调 `queryFileMeta(context.contentResolver, uri)` 提取 metadata。
 */
fun queryFileMeta(resolver: ContentResolver, uri: Uri): UploadFileMeta {
    val (name, size) = resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) {
            return@use "" to 0L
        }
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        val n = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
        val s = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
        n to s
    } ?: ("" to 0L)
    return UploadFileMeta(uri = uri, displayName = name, sizeBytes = size)
}
