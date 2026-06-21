package com.starvault.ui.search

import com.starvault.data.model.FileType
import com.starvault.data.repository.toFileType
import com.starvault.ui.files.FileEntry

/**
 * 搜索屏 UiState（参考 Lumen `SearchScreen` 风格 + Files 屏 FilesUiState 思路）。
 *
 *  - query 字段贯穿所有状态（即使是 Empty / Error 状态，用户也能继续输入）
 *  - 状态机：
 *    - Idle(query)       : 用户输入但还没搜（空 query / 初始状态）
 *    - Searching(query)  : 正在拉首页
 *    - Success(query, results, ...) : 拉到了
 *    - Empty(query)      : 搜了但无结果
 *    - Error(query, msg) : 拉失败
 *
 *  设计要点：
 *  - query 不与 UiState 子状态耦合——用户输入时即使 Searching → Idle 切换也无闪烁
 *  - isLoadingMore / hasMore 内嵌在 Success 中（分页）
 *  - tabCounts / selectedIds 留 MVP 不做（搜索屏不显示 tab + 不支持多选）
 */
sealed interface SearchUiState {
    /** 当前输入框中的关键词；所有子状态都必须有。 */
    val query: String

    /**
     * Idle：用户输入了字符但尚未触发搜索（debounce 期内 / 空 query）。
     *  - 空 query → UI 显示「输入文件名搜索」提示
     *  - 非空 query 但在 debounce 内 → 仍显示提示，不显示 loading
     */
    data class Idle(override val query: String) : SearchUiState

    /**
     * 首次搜索进行中（debounce 已触发，正在 GET /files/search）。
     *  - UI 显示顶部 LinearProgressIndicator（不显示结果列表）
     */
    data class Searching(override val query: String) : SearchUiState

    /**
     * 搜索成功（有结果 + 还能分页）。
     *  - 滚动到底自动 loadMore 追加结果
     *  - totalServerCount 暂不显示（115 count 不可信，参考 FilesViewModel 经验）
     */
    data class Success(
        override val query: String,
        val results: List<FileEntry>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
    ) : SearchUiState

    /**
     * 搜索成功但 0 结果。
     *  - UI 显示 "未找到结果" 占位
     */
    data class Empty(override val query: String) : SearchUiState

    /**
     * 搜索失败（HTTP / 业务 / 网络异常）。
     *  - UI 显示错误文案 + 返回按钮
     */
    data class Error(override val query: String, val message: String) : SearchUiState
}

/**
 * 把 115 ParsedFileItem 映射成 UI FileEntry（搜索结果用）。
 * 复用 FilesViewModel 的 [com.starvault.data.repository.toFileType] 扩展。
 */
internal fun com.starvault.data.remote.cloud115.ParsedFileItem.toSearchEntry(): FileEntry {
    val type = this.toFileType()
    val sizeText = if (isFolder) "—" else formatBytes(sizeBytes)
    val durationText = when {
        isFolder -> ""
        type == FileType.VIDEO || type == FileType.AUDIO -> formatDuration(playLong)
        else -> ""
    }
    val timeText = formatRelativeTime(mtimeSec)
    val meta = mutableListOf<String>()
    if (sizeText.isNotEmpty() && sizeText != "—") meta += sizeText
    if (durationText.isNotEmpty()) meta += durationText
    if (timeText.isNotEmpty()) meta += timeText
    val thumb = when {
        isFolder -> null
        type == FileType.IMAGE || type == FileType.VIDEO -> thumbnailUrl.takeIf { it.isNotEmpty() }
        else -> null
    }
    return FileEntry(
        id = id,
        name = name,
        type = type,
        metaSegments = meta,
        isFolder = isFolder,
        thumbnailUrl = thumb,
    )
}

/* ───── helpers（与 FilesViewModel 顶部保持同款，避免重复定义） ───── */

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.1f GB".format(bytes.toDouble() / gb)
        bytes >= mb -> "%.1f MB".format(bytes.toDouble() / mb)
        bytes >= kb -> "%.1f KB".format(bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatRelativeTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSec
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60} 分钟前"
        diff < 24 * 3600 -> "${diff / 3600} 小时前"
        diff < 7 * 24 * 3600 -> "${diff / (24 * 3600)} 天前"
        else -> "%04d/%02d/%02d".format(
            (epochSec / 31536000) + 1970,
            (epochSec / 2592000) % 12 + 1,
            (epochSec / 86400) % 31 + 1,
        )
    }
}