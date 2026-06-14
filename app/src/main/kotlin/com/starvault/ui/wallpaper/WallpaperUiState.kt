package com.starvault.ui.wallpaper

import androidx.compose.ui.graphics.Color

/**
 * Wallpaper 屏 UiState（对应 design/08-wallpaper.html）。
 *
 *  - Loading : 占位
 *  - Success : 开关 + 4 个 section + footer
 *  - Error   : 拉取失败
 */
sealed interface WallpaperUiState {

    data class Loading(
        val placeholder: Unit = Unit,
    ) : WallpaperUiState

    data class Success(
        val enabled: Boolean,                   // 引擎总开关
        val mode: Mode,                         // 切换频率
        val album: AlbumRef,                    // 当前来源相册
        val display: DisplayMode,               // 显示模式
        val liveWallpaper: Boolean,             // 动态效果
        val albumOptions: List<AlbumRef>,       // sheet 候选
        val childrenOf: Map<String, List<AlbumRef>>,
        val sheet: WallpaperSheetState,
    ) : WallpaperUiState

    data class Error(
        val message: String,
    ) : WallpaperUiState
}

/* ───────────────────── 子模型 ───────────────────── */

/** 切换频率（结构化对象）。 */
sealed interface Mode {
    val label: String

    data object Unlock : Mode { override val label: String = "每次解锁" }
    data object Manual : Mode { override val label: String = "仅手动" }
    data class Interval(val value: Int, val unit: IntervalUnit) : Mode {
        override val label: String = "每 $value ${unit.label}"
    }
    data class Daily(val time: String) : Mode {       // "09:00"
        override val label: String = "每天 $time"
    }
}

enum class IntervalUnit(val label: String) {
    MINUTE("分钟"), HOUR("小时"), DAY("天");
}

/** 显示模式。 */
enum class DisplayMode(val value: String, val label: String) {
    CROP("crop",       "居中裁剪"),
    STRETCH("stretch", "拉伸填充"),
    FILL_CROP("fill-crop", "填充裁剪"),
    FULL("full",       "完整显示"),
    CENTER("center",   "居中显示");
}

/** 一个相册（与 Album 屏共享数据结构）。 */
data class AlbumRef(
    val id: String,
    val name: String,
    val color: Color,
    val photoCount: Int,
)

/* ───────────────────── Sheet 状态 ───────────────────── */

sealed interface WallpaperSheetState {
    data object Closed : WallpaperSheetState

    /** 选择相册 */
    data class AlbumPicker(
        val currentAlbumId: String,
    ) : WallpaperSheetState

    /** 切换频率（4 行 + 内嵌 input） */
    data object ModePicker : WallpaperSheetState

    /** 显示模式（5 行） */
    data class DisplayPicker(
        val currentValue: String,
    ) : WallpaperSheetState
}
