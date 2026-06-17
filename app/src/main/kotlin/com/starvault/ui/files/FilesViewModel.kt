package com.starvault.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.toFileType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Files 屏 ViewModel — 接 115 webapi /files 真实数据。
 *
 *  - init { loadFolder("0") } : 启动即拉根目录
 *  - setFolder(cid) : 跳进子文件夹；切换时取消旧 loadJob + 清选中
 *  - refresh() : 当前目录重新拉（pull-to-refresh 触发）
 *  - selectType / changeViewMode / toggleSelect / clearSelection / bulk 仍走本地态（不上行）
 *
 *  状态机：
 *    Loading → Success（list 非空）
 *            → Success（list 空但请求成功，显示空状态）
 *            → Error（请求失败，message 来自异常）
 *
 *  错误策略：
 *    网络/解析失败 → FilesUiState.Error(message)；UI 屏已有 Error 分支处理
 *    partial-success（仅文件夹或仅文件成功）由 [FilesRepository] 内部消化，对 UI 是正常 Success
 */
class FilesViewModel(
    private val filesRepository: FilesRepository = ServiceLocator.filesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FilesUiState>(FilesUiState.Loading())
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    /** 当前目录 cid；用于 setFolder / refresh 复用。 */
    private var currentCid: String = "0"

    /** 当前正在跑的拉取任务；切换目录或 refresh 时取消旧任务。 */
    private var loadJob: Job? = null

    init {
        loadJob = viewModelScope.launch { loadFolder("0") }
    }

    /** 切换目录（点击文件夹行触发）。 */
    fun setFolder(folderId: String?) {
        val cid = folderId ?: "0"
        if (cid == currentCid && _state.value is FilesUiState.Success) return
        currentCid = cid
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(cid) }
    }

    /** 重新拉当前目录（pull-to-refresh）。 */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(currentCid) }
    }

    private suspend fun loadFolder(cid: String) {
        // 切目录/refresh 时，先显示 Loading；保留旧选中集合（防止选中态意外消失）
        val previousSelected = (_state.value as? FilesUiState.Success)?.selectedIds ?: emptySet()
        val previousViewMode = (_state.value as? FilesUiState.Success)?.viewMode ?: ViewMode.LIST
        val previousSort = (_state.value as? FilesUiState.Success)?.sortLabel ?: "按修改时间"
        _state.value = FilesUiState.Loading()

        filesRepository.listFolder(cid)
            .onSuccess { items ->
                val entries = items.map { it.toFileEntry() }
                _state.value = FilesUiState.Success(
                    folderId = cid,
                    all = entries,
                    activeType = null,
                    viewMode = previousViewMode,
                    selectedIds = previousSelected.filterSelected(entries).toSet(),
                    sortLabel = previousSort,
                    totalCount = entries.size,
                )
            }
            .onFailure { e ->
                _state.value = FilesUiState.Error(
                    message = e.message ?: "文件列表加载失败",
                )
            }
    }

    /* ─────────────────── 本地态（不上行） ─────────────────── */

    fun selectType(type: FileType?) {
        val s = _state.value as? FilesUiState.Success ?: return
        _state.value = s.copy(
            activeType = type,
            totalCount = s.all.count { e -> type == null || e.type == type },
        )
    }

    fun changeViewMode(mode: ViewMode) {
        val s = _state.value as? FilesUiState.Success ?: return
        _state.value = s.copy(viewMode = mode)
    }

    fun toggleSelect(id: String) {
        val s = _state.value as? FilesUiState.Success ?: return
        val next = s.selectedIds.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
        _state.value = s.copy(selectedIds = next)
    }

    fun clearSelection() {
        val s = _state.value as? FilesUiState.Success ?: return
        _state.value = s.copy(selectedIds = emptySet())
    }

    fun bulk(action: BulkAction) {
        // Phase 2 stub：仅清掉选中；后续切片接入真 bulk endpoint
        clearSelection()
    }

    /* ─────────────────── ParsedFileItem → FileEntry 映射 ─────────────────── */

    /**
     * 把 115 DTO 映射成 UI 用的 [FileEntry]，附带 metaSegments（屏幕渲染时拼 " · "）。
     *
     *  策略：
     *  - 文件夹: [子项数（暂无，先用 "—"） , 相对时间]
     *    — FilesRepository 不返回子项数（115 /files count 是当前筛选下的条数而非子项数），
     *    暂显示 "—"；后续切片可调 /files?cid=...&fc=count 单独拿
     *  - 文件: [格式化大小, 时长（audio/video） / "—", 相对时间]
     */
    private fun ParsedFileItem.toFileEntry(): FileEntry {
        val type = toFileType()
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
        return FileEntry(
            id = id,
            name = name,
            type = type,
            metaSegments = meta,
            isFolder = isFolder,
        )
    }
}

/** 字节数 → "1.04 TB" / "32.4 MB" 风格（1 位小数）。与 ProfileViewModel 同款实现。 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024
    return when {
        bytes >= tb -> "%.1f TB".format(bytes.toDouble() / tb)
        bytes >= gb -> "%.1f GB".format(bytes.toDouble() / gb)
        bytes >= mb -> "%.1f MB".format(bytes.toDouble() / mb)
        bytes >= kb -> "%.1f KB".format(bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

/** 秒数 → "01:30:08" 或 "04:23"。 */
private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = TimeUnit.SECONDS.toHours(seconds.toLong())
    val m = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Unix 秒 → 中文相对时间（"2 小时前" / "昨天 23:14" / "今天 14:22" / "3 天前" / "2026/06/14"）。 */
private fun formatRelativeTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSec
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60} 分钟前"
        diff < 24 * 3600 -> "${diff / 3600} 小时前"
        diff < 7 * 24 * 3600 -> "${diff / (24 * 3600)} 天前"
        else -> {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochSec * 1000 }
            "%04d/%02d/%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
            )
        }
    }
}

/** 切目录后过滤掉不属于新列表的 id（防止选中态泄漏）。 */
private fun Set<String>.filterSelected(currentEntries: List<FileEntry>): List<String> {
    val valid = currentEntries.map { it.id }.toSet()
    return this.filter { it in valid }
}

enum class BulkAction { DOWNLOAD, SHARE, MOVE, RENAME, DELETE }