package com.starvault.ui.album

import androidx.compose.ui.graphics.Color

/**
 * Album 屏 UiState（对应 design/07-album.html）。
 *
 *  - Loading : 占位
 *  - Success : 相册 + 子目录 chip + 日期分组 + 照片网格
 *  - Error   : 拉取失败(Phase 1 mock 不再使用,改走 ToastBus 错误提示)
 */
sealed interface AlbumUiState {

    data class Loading(
        val placeholder: Unit = Unit,
    ) : AlbumUiState

    data class Success(
        val currentAlbum: AlbumFolder,
        val subTabs: List<SubTab>,
        val activeSubTab: SubTabId,
        val dateGroups: List<DateGroup>,
        val folderSheet: FolderSheetState,
    ) : AlbumUiState
}

/* ───────────────────── 子模型 ───────────────────── */

/**
 * 一个相册（一级或子目录）。Phase 1 把相册列表硬编码在 ViewModel，
 * 真实接入 115 后改读 /album/list。
 */
data class AlbumFolder(
    val id: String,
    val name: String,
    val color: Color,
    val photoCount: Int,
    val videoCount: Int = 0,
    val totalGb: String,        // "4.2 GB" / "12.3 GB"
)

/** 子目录快捷筛选 tab（不是智能分类，是文件属性）。 */
data class SubTab(
    val id: SubTabId,
    val label: String,
)

enum class SubTabId { ALL, FAVORITES, RECENT_UPLOAD, RAW }

/** 1 个日期分组（"今天·6 月 7 日" / "6 月 3 日"）。 */
data class DateGroup(
    val id: String,             // 用作 LazyColumn key
    val label: String,          // "今天"
    val subLabel: String?,      // "6 月 7 日"（今天/昨天才显示）
    val photos: List<PhotoEntry>,
)

/** 网格中的单张。 */
data class PhotoEntry(
    val id: String,
    val kind: PhotoKind,
    val scene: PhotoScene,      // 决定 1×1 cell 的渐变背景
    val timeLabel: String?,     // 右下角白色 "14:32" / "0:24" / null
    val isFavorite: Boolean,    // 右上角红心
)

enum class PhotoKind { PHOTO, VIDEO, SCREENSHOT, DOCUMENT }

/** 10 个场景 → 5 段渐变背景（与 design 1:1）。 */
enum class PhotoScene {
    SUNSET, FOREST, MOUNTAIN, COFFEE, BLDG, NIGHT, FOOD, PORTRAIT, BEACH, DOC,
}

/* ───────────────────── 文件夹选择 Sheet ───────────────────── */

/** Sheet 状态：关闭 / 打开中。 */
sealed interface FolderSheetState {
    data object Closed : FolderSheetState
    data class Open(
        val albums: List<AlbumFolder>,           // 一级相册
        val childrenOf: Map<String, List<AlbumFolder>>,  // 父 → 子
        val recents: List<AlbumFolder>,          // 最近使用 chip
        val currentId: String,                   // 当前选中
    ) : FolderSheetState
}
