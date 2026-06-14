package com.starvault.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Album 屏 Preview — 2 个 case：
 *  1. Default  完整我的相册
 *  2. Sheet    FolderSheet 打开
 */
@Preview(name = "Album/Default", showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun AlbumDefaultPreview() = StarVaultTheme {
    AlbumScreen(state = albumPreviewSuccess(closed = true))
}

@Preview(name = "Album/Sheet",   showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun AlbumSheetPreview() = StarVaultTheme {
    AlbumScreen(state = albumPreviewSuccess(closed = false))
}

internal fun albumPreviewSuccess(closed: Boolean): AlbumUiState.Success = AlbumUiState.Success(
    currentAlbum = AlbumFolder("mine", "我的相册", androidx.compose.ui.graphics.Color(0xFF2F6FEB), 1247, 23, "4.2 GB"),
    subTabs = listOf(
        SubTab(SubTabId.ALL, "全部"),
        SubTab(SubTabId.FAVORITES, "收藏"),
        SubTab(SubTabId.RECENT_UPLOAD, "最近上传"),
        SubTab(SubTabId.RAW, "原图"),
    ),
    activeSubTab = SubTabId.ALL,
    dateGroups = listOf(
        DateGroup("today", "今天", "6 月 7 日", listOf(
            PhotoEntry("p-01", PhotoKind.PHOTO, PhotoScene.COFFEE,  "14:32", false),
            PhotoEntry("p-02", PhotoKind.PHOTO, PhotoScene.BLDG,    "10:15", false),
            PhotoEntry("p-03", PhotoKind.VIDEO, PhotoScene.NIGHT,   "0:24",  false),
        )),
        DateGroup("yesterday", "昨天", "6 月 6 日", listOf(
            PhotoEntry("p-04", PhotoKind.PHOTO,       PhotoScene.FOOD,     null, false),
            PhotoEntry("p-05", PhotoKind.SCREENSHOT,  PhotoScene.PORTRAIT, null, false),
            PhotoEntry("p-06", PhotoKind.PHOTO,       PhotoScene.PORTRAIT, null, true),
        )),
        DateGroup("jun-05", "6 月 5 日", null, listOf(
            PhotoEntry("p-07", PhotoKind.PHOTO, PhotoScene.SUNSET,   null, true),
            PhotoEntry("p-08", PhotoKind.PHOTO, PhotoScene.FOREST,   null, false),
            PhotoEntry("p-09", PhotoKind.PHOTO, PhotoScene.MOUNTAIN, null, false),
        )),
        DateGroup("jun-03", "6 月 3 日", null, listOf(
            PhotoEntry("p-10", PhotoKind.PHOTO,    PhotoScene.BEACH,    null, false),
            PhotoEntry("p-11", PhotoKind.DOCUMENT, PhotoScene.DOC,      "PDF", false),
            PhotoEntry("p-12", PhotoKind.PHOTO,    PhotoScene.PORTRAIT, null, false),
        )),
    ),
    folderSheet = if (closed) FolderSheetState.Closed else FolderSheetState.Open(
        albums = listOf(
            AlbumFolder("mine",   "我的相册",   androidx.compose.ui.graphics.Color(0xFF2F6FEB), 1247, 23, "4.2 GB"),
            AlbumFolder("travel", "旅行 2025", androidx.compose.ui.graphics.Color(0xFF16A34A), 328, 0, "1.8 GB"),
            AlbumFolder("family", "家庭",      androidx.compose.ui.graphics.Color(0xFFF59E0B), 156, 0, "0.9 GB"),
            AlbumFolder("work",   "工作",      androidx.compose.ui.graphics.Color(0xFF8B5CF6), 47, 0, "0.3 GB"),
            AlbumFolder("fav",    "收藏",      androidx.compose.ui.graphics.Color(0xFFEF4444), 23, 0, "0.1 GB"),
        ),
        childrenOf = mapOf(
            "mine" to listOf(
                AlbumFolder("mine-2025", "2025", androidx.compose.ui.graphics.Color(0xFF2F6FEB), 328, 0, "1.1 GB"),
                AlbumFolder("mine-2024", "2024", androidx.compose.ui.graphics.Color(0xFF2F6FEB), 612, 0, "2.4 GB"),
            ),
            "family" to listOf(
                AlbumFolder("family-baby", "宝宝成长", androidx.compose.ui.graphics.Color(0xFFEC4899), 89, 0, "0.4 GB"),
            ),
        ),
        recents = listOf(
            AlbumFolder("travel", "旅行 2025", androidx.compose.ui.graphics.Color(0xFF16A34A), 328, 0, "1.8 GB"),
            AlbumFolder("family", "家庭",      androidx.compose.ui.graphics.Color(0xFFF59E0B), 156, 0, "0.9 GB"),
        ),
        currentId = "mine",
    ),
)
