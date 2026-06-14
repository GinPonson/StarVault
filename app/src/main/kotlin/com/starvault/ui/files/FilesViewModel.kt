package com.starvault.ui.files

import androidx.lifecycle.ViewModel
import com.starvault.data.model.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Files 屏 ViewModel — Phase 1 mock。
 *
 * 真实接入 115 后：
 *  - list(folderId) ← /files?folderId=...
 *  - selectType / changeViewMode / toggleSelect / selectAll / clearSelection 走本地态
 *  - bulkAction(download/share/move/rename/delete) ← 调 API
 *  - newFolder / upload ← 调 API
 *
 * 设计要点（与 06-files.html 1:1）：
 *  - 默认 ViewMode = LIST
 *  - 选中数 > 0 时 bulk-bar 出现
 *  - FOLDER 行的 meta 走 "28 项 · 2 小时前"，不展示 size
 */
class FilesViewModel : ViewModel() {

    private val _state = MutableStateFlow<FilesUiState>(mockState(folderId = null))
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    fun setFolder(folderId: String?) {
        // Phase 1：所有 tab 都共享一份根目录数据；folderId 切换时重新 mock
        _state.value = mockState(folderId)
    }

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
        // Phase 1 stub：清掉选中
        clearSelection()
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockState(folderId: String?): FilesUiState.Success {
        // 10 条 1:1 复刻 design/06-files.html
        val all = listOf(
            FileEntry("f-01", "设计交付 / 2026Q2",          FileType.FOLDER, listOf("28 项", "2 小时前"),  isFolder = true),
            FileEntry("f-02", "Final.Destination.2026.1080p.mkv", FileType.VIDEO, listOf("2.18 GB", "1:42:08", "1 小时前"), isFolder = false),
            FileEntry("f-03", "陈奕迅 - 孤勇者.flac",       FileType.AUDIO, listOf("32.4 MB", "04:23", "今天 14:22"), isFolder = false),
            FileEntry("f-04", "2026 年度产品规划 v3.2.pdf", FileType.DOC,   listOf("8.7 MB", "8 页",  "今天 09:12"),  isFolder = false),
            FileEntry("f-05", "京都樱花 / 4 月 / DSC_4821.jpg", FileType.IMAGE, listOf("14.2 MB", "4032 × 3024", "昨天 23:14"), isFolder = false),
            FileEntry("f-06", "毕业设计源码 v2.zip",         FileType.ZIP,   listOf("62.1 MB", "23 个文件", "昨天 19:30"), isFolder = false),
            FileEntry("f-07", "影视收藏",                    FileType.FOLDER, listOf("64 项", "3 天前"),  isFolder = true),
            FileEntry("f-08", "读书笔记 - 2026.md",          FileType.DOC,   listOf("142 KB", "4 天前"),  isFolder = false),
            FileEntry("f-09", "WandaVision.S01E05.mkv",      FileType.VIDEO, listOf("1.42 GB", "38:24", "5 天前"),   isFolder = false),
            FileEntry("f-10", "工作文档",                    FileType.FOLDER, listOf("17 项", "上周"),     isFolder = true),
        )
        return FilesUiState.Success(
            folderId = folderId,
            all = all,
            activeType = null,
            viewMode = ViewMode.LIST,
            selectedIds = emptySet(),
            sortLabel = "按修改时间",
            totalCount = all.size,
        )
    }
}

enum class BulkAction { DOWNLOAD, SHARE, MOVE, RENAME, DELETE }
