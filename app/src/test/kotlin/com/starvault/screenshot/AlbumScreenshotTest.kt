package com.starvault.screenshot

import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.album.AlbumFolder
import com.starvault.ui.album.AlbumScreen
import com.starvault.ui.album.AlbumUiState
import com.starvault.ui.album.DateGroup
import com.starvault.ui.album.FolderSheetState
import com.starvault.ui.album.PhotoEntry
import com.starvault.ui.album.PhotoKind
import com.starvault.ui.album.PhotoScene
import com.starvault.ui.album.SubTab
import com.starvault.ui.album.SubTabId
import org.junit.Rule
import org.junit.Test

/**
 * Album 屏 Paparazzi 回归基线 — 与 design/07-album.html 对齐。
 *
 *  - ready   : 默认主态（"我的相册" 选中，3 个日期分组）
 *  - fav     : "收藏" subTab 选中
 *  - sheet   : 相册选择 sheet 打开
 */
class AlbumScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private val mine = AlbumFolder(
        id = "mine", name = "我的相册",
        color = Color(0xFF2F6FEB), photoCount = 1247, videoCount = 23, totalGb = "4.2 GB",
    )
    private val travel = AlbumFolder(
        id = "travel", name = "旅行 2025",
        color = Color(0xFF16A34A), photoCount = 328, totalGb = "1.8 GB",
    )
    private val family = AlbumFolder(
        id = "family", name = "家庭",
        color = Color(0xFFF59E0B), photoCount = 156, totalGb = "0.9 GB",
    )
    private val work = AlbumFolder(
        id = "work", name = "工作",
        color = Color(0xFF8B5CF6), photoCount = 47, totalGb = "0.3 GB",
    )

    private val mockDateGroups = listOf(
        DateGroup(
            id = "today", label = "今天", subLabel = "6 月 7 日",
            photos = listOf(
                PhotoEntry("p-01", PhotoKind.PHOTO, PhotoScene.COFFEE, "14:32", false),
                PhotoEntry("p-02", PhotoKind.PHOTO, PhotoScene.BLDG, "10:15", false),
                PhotoEntry("p-03", PhotoKind.VIDEO, PhotoScene.NIGHT, "0:24", false),
            ),
        ),
        DateGroup(
            id = "yesterday", label = "昨天", subLabel = "6 月 6 日",
            photos = listOf(
                PhotoEntry("p-04", PhotoKind.PHOTO, PhotoScene.FOOD, null, false),
                PhotoEntry("p-05", PhotoKind.SCREENSHOT, PhotoScene.PORTRAIT, null, false),
                PhotoEntry("p-06", PhotoKind.PHOTO, PhotoScene.PORTRAIT, null, true),
            ),
        ),
        DateGroup(
            id = "jun-05", label = "6 月 5 日", subLabel = null,
            photos = listOf(
                PhotoEntry("p-07", PhotoKind.PHOTO, PhotoScene.SUNSET, null, true),
                PhotoEntry("p-08", PhotoKind.PHOTO, PhotoScene.FOREST, null, false),
                PhotoEntry("p-09", PhotoKind.PHOTO, PhotoScene.MOUNTAIN, null, false),
            ),
        ),
    )

    private val mockSubTabs = listOf(
        SubTab(SubTabId.ALL, "全部"),
        SubTab(SubTabId.FAVORITES, "收藏"),
        SubTab(SubTabId.RECENT_UPLOAD, "最近上传"),
        SubTab(SubTabId.RAW, "原图"),
    )

    @Test fun album_ready() = paparazzi.snapshot {
        StarVaultTheme {
            AlbumScreen(
                state = AlbumUiState.Success(
                    currentAlbum = mine,
                    subTabs = mockSubTabs,
                    activeSubTab = SubTabId.ALL,
                    dateGroups = mockDateGroups,
                    folderSheet = FolderSheetState.Closed,
                ),
            )
        }
    }

    @Test fun album_fav() = paparazzi.snapshot {
        StarVaultTheme {
            AlbumScreen(
                state = AlbumUiState.Success(
                    currentAlbum = mine,
                    subTabs = mockSubTabs,
                    activeSubTab = SubTabId.FAVORITES,
                    dateGroups = mockDateGroups,
                    folderSheet = FolderSheetState.Closed,
                ),
            )
        }
    }

    @Test fun album_sheet() = paparazzi.snapshot {
        StarVaultTheme {
            AlbumScreen(
                state = AlbumUiState.Success(
                    currentAlbum = mine,
                    subTabs = mockSubTabs,
                    activeSubTab = SubTabId.ALL,
                    dateGroups = mockDateGroups,
                    folderSheet = FolderSheetState.Open(
                        albums = listOf(mine, travel, family, work),
                        childrenOf = mapOf(
                            "mine" to listOf(
                                AlbumFolder("mine-2025", "2025", Color(0xFF2F6FEB), 328, totalGb = "1.1 GB"),
                                AlbumFolder("mine-2024", "2024", Color(0xFF2F6FEB), 612, totalGb = "2.4 GB"),
                            ),
                            "family" to listOf(
                                AlbumFolder("family-baby", "宝宝成长", Color(0xFFEC4899), 89, totalGb = "0.4 GB"),
                            ),
                        ),
                        recents = listOf(travel, family),
                        currentId = "mine",
                    ),
                ),
            )
        }
    }
}