package com.starvault.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

/**
 * Home 屏 Preview — 4 个 case：
 *  1. 默认（activeTag = null）        ：6 条文件全显
 *  2. 选中 "工作" tag                ：仅显示带 工作 tag 的文件
 *  3. Loading                        ：文件区空
 *  4. Error                          ：文件区显示 message
 *
 * widthDp/heightDp = 412x900，与 design HTML device frame 一致（Paparazzi T24 会回归）。
 */

@Preview(name = "Home/Default",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun HomeDefaultPreview() = StarVaultTheme {
    HomeScreen(
        state = HomeUiState.Success(
            files = homePreviewFiles(),
            relTimes = mapOf(
                "h-01" to "2 小时前",
                "h-02" to "1:42:08",
                "h-03" to "04:23",
                "h-04" to "今天 09:12",
                "h-05" to "4032 × 3024",
                "h-06" to "昨天 23:14",
            ),
        ),
        onTagClick = {},
        onAllTagClick = {},
        onSortClick = {},
        onFabClick = {},
        onFileClick = {},
        onFileMore = {},
        onQuickClick = {},
    )
}

@Preview(name = "Home/Tag=工作", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun HomeWorkTagPreview() = StarVaultTheme {
    HomeScreen(
        state = HomeUiState.Success(
            files = homePreviewFiles(),
            activeTag = FileTag("工作", TagColor.TAG1),
            relTimes = mapOf(
                "h-01" to "2 小时前",
                "h-04" to "今天 09:12",
            ),
        ),
        onTagClick = {},
        onAllTagClick = {},
        onSortClick = {},
        onFabClick = {},
        onFileClick = {},
        onFileMore = {},
        onQuickClick = {},
    )
}

@Preview(name = "Home/Loading",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun HomeLoadingPreview() = StarVaultTheme {
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

@Preview(name = "Home/Error",    showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun HomeErrorPreview() = StarVaultTheme {
    HomeScreen(
        state = HomeUiState.Error(message = "网络异常，请稍后重试"),
        onTagClick = {},
        onAllTagClick = {},
        onSortClick = {},
        onFabClick = {},
        onFileClick = {},
        onFileMore = {},
        onQuickClick = {},
    )
}

/** 与 ViewModel.homeDesignFiles() 1:1 复刻，供 Preview 使用。*/
private fun homePreviewFiles(): List<FileItem> = listOf(
    FileItem("h-01", "设计交付 / 2026Q2",            FileType.FOLDER, durationOrCount = "28 项",         mtime = 1_718_000_000_000L, tag = FileTag("工作", TagColor.TAG1)),
    FileItem("h-02", "Final.Destination.2026.1080p", FileType.VIDEO,  sizeBytes = 2_340_234_240L, durationOrCount = "1:42:08",    mtime = 1_717_990_000_000L, tag = FileTag("影视", TagColor.TAG3)),
    FileItem("h-03", "陈奕迅 - 孤勇者.flac",         FileType.AUDIO,  sizeBytes = 33_976_320L,   durationOrCount = "04:23",      mtime = 1_717_980_000_000L, tag = FileTag("音乐", TagColor.TAG5)),
    FileItem("h-04", "2026 年度产品规划 v3.2.pdf",   FileType.DOC,    sizeBytes = 9_126_400L,                                       mtime = 1_717_970_000_000L, tag = FileTag("工作", TagColor.TAG1)),
    FileItem("h-05", "京都樱花 / 4 月 / DSC_4821",   FileType.IMAGE,  sizeBytes = 14_901_248L,   durationOrCount = "4032 × 3024",mtime = 1_717_960_000_000L, tag = FileTag("生活", TagColor.TAG2)),
    FileItem("h-06", "毕业设计源码 v2.zip",          FileType.ZIP,    sizeBytes = 65_142_784L,                                       mtime = 1_717_950_000_000L, tag = FileTag("学习", TagColor.TAG2)),
)
