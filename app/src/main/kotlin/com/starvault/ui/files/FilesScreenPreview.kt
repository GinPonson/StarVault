package com.starvault.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Files 屏 Preview — 3 个 case：
 *  1. Default       列表视图
 *  2. Selected      选中 2 条 → bulk-bar 浮出
 *  3. Grid          网格视图（2 列）
 */
@Preview(name = "Files/List",    showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun FilesListPreview() = StarVaultTheme {
    FilesScreen(state = filesPreviewSuccess())
}

@Preview(name = "Files/Selected", showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun FilesSelectedPreview() = StarVaultTheme {
    FilesScreen(state = filesPreviewSuccess().copy(
        selectedIds = setOf("f-02", "f-04"),
    ))
}

@Preview(name = "Files/Grid",    showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun FilesGridPreview() = StarVaultTheme {
    FilesScreen(state = filesPreviewSuccess().copy(viewMode = ViewMode.GRID))
}

internal fun filesPreviewSuccess(): FilesUiState.Success = FilesUiState.Success(
    folderId = null,
    all = listOf(
        FileEntry("f-01", "设计交付 / 2026Q2",          com.starvault.data.model.FileType.FOLDER, listOf("28 项", "2 小时前"),  isFolder = true),
        FileEntry("f-02", "Final.Destination.2026.1080p.mkv", com.starvault.data.model.FileType.VIDEO, listOf("2.18 GB", "1:42:08", "1 小时前"), isFolder = false),
        FileEntry("f-03", "陈奕迅 - 孤勇者.flac",       com.starvault.data.model.FileType.AUDIO, listOf("32.4 MB", "04:23", "今天 14:22"), isFolder = false),
        FileEntry("f-04", "2026 年度产品规划 v3.2.pdf", com.starvault.data.model.FileType.DOC,   listOf("8.7 MB", "8 页",  "今天 09:12"),  isFolder = false),
        FileEntry("f-05", "京都樱花 / 4 月 / DSC_4821.jpg", com.starvault.data.model.FileType.IMAGE, listOf("14.2 MB", "4032 × 3024", "昨天 23:14"), isFolder = false),
        FileEntry("f-06", "毕业设计源码 v2.zip",         com.starvault.data.model.FileType.ZIP,   listOf("62.1 MB", "23 个文件", "昨天 19:30"), isFolder = false),
        FileEntry("f-07", "影视收藏",                    com.starvault.data.model.FileType.FOLDER, listOf("64 项", "3 天前"),  isFolder = true),
        FileEntry("f-08", "读书笔记 - 2026.md",          com.starvault.data.model.FileType.DOC,   listOf("142 KB", "4 天前"),  isFolder = false),
        FileEntry("f-09", "WandaVision.S01E05.mkv",      com.starvault.data.model.FileType.VIDEO, listOf("1.42 GB", "38:24", "5 天前"),   isFolder = false),
        FileEntry("f-10", "工作文档",                    com.starvault.data.model.FileType.FOLDER, listOf("17 项", "上周"),     isFolder = true),
    ),
    activeType = null,
    viewMode = ViewMode.LIST,
    selectedIds = emptySet(),
    sortLabel = "按修改时间",
    totalCount = 10,
)
