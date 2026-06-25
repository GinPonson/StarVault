package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.home.HomeScreen
import com.starvault.ui.home.HomeUiState
import org.junit.Rule
import org.junit.Test

/**
 * Home 屏 Paparazzi 回归基线 — 与 design/01-home.html 对齐。
 *
 *  - ready   : 默认主态（5 条 mock 文件 + 默认 FileTag）
 *  - loading : Loading 占位（无文件列表）
 */
class HomeScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    @Test fun home_ready() = paparazzi.snapshot {
        StarVaultTheme {
            HomeScreen(
                state = HomeUiState.Success(files = mockHomeFiles()),
                onTagClick = {},
                onAllTagClick = {},
                onSortClick = {},
                onFabClick = {},
                onFileClick = {},
                onFileMore = {},
                onQuickClick = {},
            )
        }
    }

    @Test fun home_loading() = paparazzi.snapshot {
        StarVaultTheme {
            HomeScreen(
                state = HomeUiState.Loading(),
                onTagClick = {},
                onAllTagClick = {},
                onSortClick = {},
                onFabClick = {},
                onFileClick = {},
                onFileMore = {},
                onQuickClick = {},
            )
        }
    }

    // 5 条覆盖 FOLDER / VIDEO / IMAGE / DOC / AUDIO 的最小 mock,与 design/01-home.html 一致
    private fun mockHomeFiles(): List<FileItem> = listOf(
        FileItem("f01", "旅行 2025",      FileType.FOLDER, durationOrCount = "38 项",   mtime = 1_718_000_000_000, tag = FileTag("生活", TagColor.TAG2)),
        FileItem("f02", "东京 vlog.mp4",  FileType.VIDEO,  sizeBytes = 104_857_600, durationOrCount = "08:42", mtime = 1_717_900_000_000, tag = FileTag("生活", TagColor.TAG2)),
        FileItem("f03", "海边日落.jpg",   FileType.IMAGE,  sizeBytes = 2_097_152,  durationOrCount = "4032 × 3024", mtime = 1_717_800_000_000, tag = FileTag("影视", TagColor.TAG3)),
        FileItem("f04", "会议纪要.docx",  FileType.DOC,    sizeBytes = 40_960,    mtime = 1_717_700_000_000),
        FileItem("f05", "钢琴曲.wav",     FileType.AUDIO,  sizeBytes = 31_457_280, durationOrCount = "02:18", mtime = 1_717_600_000_000, tag = FileTag("音乐", TagColor.TAG5)),
    )
}