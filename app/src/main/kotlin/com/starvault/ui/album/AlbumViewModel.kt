package com.starvault.ui.album

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Album 屏 ViewModel — Phase 1 mock。
 *
 * 真实接入 115 后：
 *  - albums / children / recents ← /album/list
 *  - dateGroups ← /album/photos?albumId=...&tab=...
 *  - selectAlbum / selectSubTab / openSheet / closeSheet / selectFromSheet 走本地态
 */
class AlbumViewModel : ViewModel() {

    private val _state = MutableStateFlow<AlbumUiState>(mockInitialState())
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    fun openSheet() {
        val s = _state.value as? AlbumUiState.Success ?: return
        // recents 是 mock 写死的两个固定 id,用 requireNotNull 替代 !! 便于排错
        val travel = requireNotNull(albumsById("travel")) { "missing mock album: travel" }
        val family = requireNotNull(albumsById("family")) { "missing mock album: family" }
        _state.value = s.copy(folderSheet = FolderSheetState.Open(
            albums = allAlbums(),
            childrenOf = childrenMap(),
            recents = listOf(travel, family),
            currentId = s.currentAlbum.id,
        ))
    }

    fun closeSheet() {
        val s = _state.value as? AlbumUiState.Success ?: return
        _state.value = s.copy(folderSheet = FolderSheetState.Closed)
    }

    fun selectFromSheet(id: String) {
        val s = _state.value as? AlbumUiState.Success ?: return
        val target = albumsById(id) ?: return
        _state.value = s.copy(
            currentAlbum = target,
            folderSheet = FolderSheetState.Closed,
        )
    }

    fun selectSubTab(tab: SubTabId) {
        val s = _state.value as? AlbumUiState.Success ?: return
        _state.value = s.copy(activeSubTab = tab)
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockInitialState(): AlbumUiState.Success {
        val current = albumsById("mine") ?: allAlbums().first()
        return AlbumUiState.Success(
            currentAlbum = current,
            subTabs = listOf(
                SubTab(SubTabId.ALL,           "全部"),
                SubTab(SubTabId.FAVORITES,     "收藏"),
                SubTab(SubTabId.RECENT_UPLOAD, "最近上传"),
                SubTab(SubTabId.RAW,           "原图"),
            ),
            activeSubTab = SubTabId.ALL,
            dateGroups = mockDateGroups(),
            folderSheet = FolderSheetState.Closed,
        )
    }

    private fun allAlbums(): List<AlbumFolder> = listOf(
        AlbumFolder("mine",   "我的相册",   Color(0xFF2F6FEB), photoCount = 1247, videoCount = 23, totalGb = "4.2 GB"),
        AlbumFolder("travel", "旅行 2025", Color(0xFF16A34A), photoCount = 328,  videoCount = 0,  totalGb = "1.8 GB"),
        AlbumFolder("family", "家庭",      Color(0xFFF59E0B), photoCount = 156,  videoCount = 0,  totalGb = "0.9 GB"),
        AlbumFolder("work",   "工作",      Color(0xFF8B5CF6), photoCount = 47,   videoCount = 0,  totalGb = "0.3 GB"),
        AlbumFolder("fav",    "收藏",      Color(0xFFEF4444), photoCount = 23,   videoCount = 0,  totalGb = "0.1 GB"),
    )

    private fun childrenMap(): Map<String, List<AlbumFolder>> = mapOf(
        "mine" to listOf(
            AlbumFolder("mine-2025", "2025", Color(0xFF2F6FEB), photoCount = 328, totalGb = "1.1 GB"),
            AlbumFolder("mine-2024", "2024", Color(0xFF2F6FEB), photoCount = 612, totalGb = "2.4 GB"),
        ),
        "family" to listOf(
            AlbumFolder("family-baby", "宝宝成长", Color(0xFFEC4899), photoCount = 89, totalGb = "0.4 GB"),
        ),
    )

    private fun albumsById(id: String): AlbumFolder? {
        for (a in allAlbums()) {
            if (a.id == id) return a
        }
        for ((_, cs) in childrenMap()) {
            for (c in cs) {
                if (c.id == id) return c
            }
        }
        return null
    }

    private fun mockDateGroups(): List<DateGroup> = listOf(
        DateGroup(
            id = "today",
            label = "今天",
            subLabel = "6 月 7 日",
            photos = listOf(
                PhotoEntry("p-01", PhotoKind.PHOTO, PhotoScene.COFFEE,  "14:32", false),
                PhotoEntry("p-02", PhotoKind.PHOTO, PhotoScene.BLDG,    "10:15", false),
                PhotoEntry("p-03", PhotoKind.VIDEO, PhotoScene.NIGHT,   "0:24",  false),
            ),
        ),
        DateGroup(
            id = "yesterday",
            label = "昨天",
            subLabel = "6 月 6 日",
            photos = listOf(
                PhotoEntry("p-04", PhotoKind.PHOTO,       PhotoScene.FOOD,     null, false),
                PhotoEntry("p-05", PhotoKind.SCREENSHOT,  PhotoScene.PORTRAIT, null, false),
                PhotoEntry("p-06", PhotoKind.PHOTO,       PhotoScene.PORTRAIT, null, true),
            ),
        ),
        DateGroup(
            id = "jun-05",
            label = "6 月 5 日",
            subLabel = null,
            photos = listOf(
                PhotoEntry("p-07", PhotoKind.PHOTO, PhotoScene.SUNSET,   null, true),
                PhotoEntry("p-08", PhotoKind.PHOTO, PhotoScene.FOREST,   null, false),
                PhotoEntry("p-09", PhotoKind.PHOTO, PhotoScene.MOUNTAIN, null, false),
            ),
        ),
        DateGroup(
            id = "jun-03",
            label = "6 月 3 日",
            subLabel = null,
            photos = listOf(
                PhotoEntry("p-10", PhotoKind.PHOTO,    PhotoScene.BEACH,    null, false),
                PhotoEntry("p-11", PhotoKind.DOCUMENT, PhotoScene.DOC,      "PDF", false),
                PhotoEntry("p-12", PhotoKind.PHOTO,    PhotoScene.PORTRAIT, null, false),
            ),
        ),
    )
}
