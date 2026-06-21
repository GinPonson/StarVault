package com.starvault.ui.preview

import com.starvault.data.repository.MediaMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Preview 屏的 UI 状态机（IMAGE / VIDEO 共用同一套 sealed）。
 *
 *  三态：
 *  - Loading : 初始 / 拉 metadata 或 url 中
 *  - Success : 拿到可播放/可看的资源
 *  - Error   : 文件不存在 / API 失败 / 解码错误
 *
 *  为什么不让 VM 各持一套：两边业务流程同构（getInfo → 拿 URL → 渲染），抽到 shared 让
 *  Route / Screen 都能复用 isLoading / isError 的逻辑分支判断。
 */
sealed interface PreviewUiState {
    val isLoading: Boolean get() = this is Loading

    data object Loading : PreviewUiState

    data class Success(
        /** 从 /files/get_info 拿到的文件 metadata（name / size / pickCode / thumbnailUrl）。 */
        val metadata: MediaMetadata,
        /**
         * 拿到后可用的资源 URL：
         *  - IMAGE：图片原图 file_url（Coil 直接 GET）
         *  - VIDEO：m3u8 video_url（Media3 直接播放）
         */
        val mediaUrl: String,
    ) : PreviewUiState

    data class Error(val message: String) : PreviewUiState
}

/* ─────────────────── 跨 Screen 共用的格式化工具 ─────────────────── */

/**
 * 字节 → 人类可读（B / KB / MB / GB）。对齐 Lumen `ImagePreviewScreen.kt:formatFileSize`。
 *
 * @param sizeBytes 字节数；<=0 → "未知大小"
 */
internal fun formatFileSize(sizeBytes: Long): String {
    return when {
        sizeBytes <= 0 -> "未知大小"
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        sizeBytes < 1024 * 1024 * 1024 ->
            String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        else ->
            String.format(Locale.getDefault(), "%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * unix 秒 → "yyyy-MM-dd HH:mm"。对齐 Lumen `ImagePreviewScreen.kt:formatDate`。
 *
 * @param timestampSec unix 秒；<=0 → "未知日期"
 */
internal fun formatDate(timestampSec: Long): String {
    return if (timestampSec <= 0) {
        "未知日期"
    } else {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        fmt.format(Date(timestampSec * 1000L))
    }
}