package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.data.model.FileType
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.files.FileEntry
import com.starvault.ui.files.FilesScreen
import com.starvault.ui.files.FilesUiState
import com.starvault.ui.files.ViewMode
import org.junit.Rule
import org.junit.Test

/**
 * Files 屏 Paparazzi 回归基线 — 与 design/06-files.html 对齐。
 *
 *  - list  : 列表视图（默认）
 *  - grid  : 网格视图
 *  - select: 多选态（2 个文件被选中，bulk-bar 显示）
 */
class FilesScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private val mockFiles = listOf(
        FileEntry("f-01", "设计交付 / 2026Q2", FileType.FOLDER, listOf("28 项", "2 小时前"), isFolder = true),
        FileEntry("f-02", "Final.Destination.2026.1080p.mkv", FileType.VIDEO, listOf("2.18 GB", "1:42:08", "1 小时前"), isFolder = false),
        FileEntry("f-03", "陈奕迅 - 孤勇者.flac", FileType.AUDIO, listOf("32.4 MB", "04:23", "今天 14:22"), isFolder = false),
        FileEntry("f-04", "2026 年度产品规划 v3.2.pdf", FileType.DOC, listOf("8.7 MB", "8 页", "今天 09:12"), isFolder = false),
        FileEntry("f-05", "京都樱花 / 4 月 / DSC_4821.jpg", FileType.IMAGE, listOf("14.2 MB", "4032 × 3024", "昨天 23:14"), isFolder = false),
        FileEntry("f-06", "毕业设计源码 v2.zip", FileType.ZIP, listOf("62.1 MB", "23 个文件", "昨天 19:30"), isFolder = false),
    )

    private fun success(
        viewMode: ViewMode = ViewMode.LIST,
        selectedIds: Set<String> = emptySet(),
    ) = FilesUiState.Success(
        folderId = null,
        all = mockFiles,
        activeType = null,
        viewMode = viewMode,
        selectedIds = selectedIds,
        sortLabel = "按修改时间",
        totalCount = mockFiles.size,
    )

    @Test fun files_list() = paparazzi.snapshot {
        StarVaultTheme { FilesScreen(state = success()) }
    }

    @Test fun files_grid() = paparazzi.snapshot {
        StarVaultTheme { FilesScreen(state = success(viewMode = ViewMode.GRID)) }
    }

    @Test fun files_select() = paparazzi.snapshot {
        StarVaultTheme {
            FilesScreen(
                state = success(selectedIds = setOf("f-02", "f-04")),
            )
        }
    }
}