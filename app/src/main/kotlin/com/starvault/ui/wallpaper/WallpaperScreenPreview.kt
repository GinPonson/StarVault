package com.starvault.ui.wallpaper

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Wallpaper 屏 Preview — 3 个 case：
 *  1. Off           关闭态（仅 SwitchCard）
 *  2. On            开启态（4 个 section + footer）
 *  3. ModeSheet     切换频率 sheet 打开
 */
@Preview(name = "Wallpaper/Off",   showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun WallpaperOffPreview() = StarVaultTheme {
    WallpaperScreen(state = wallpaperPreviewSuccess(enabled = false))
}

@Preview(name = "Wallpaper/On",    showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun WallpaperOnPreview() = StarVaultTheme {
    WallpaperScreen(state = wallpaperPreviewSuccess(enabled = true))
}

@Preview(name = "Wallpaper/Mode",  showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun WallpaperModePreview() = StarVaultTheme {
    WallpaperScreen(state = wallpaperPreviewSuccess(enabled = true).copy(
        sheet = WallpaperSheetState.ModePicker,
    ))
}

internal fun wallpaperPreviewSuccess(enabled: Boolean): WallpaperUiState.Success = WallpaperUiState.Success(
    enabled = enabled,
    mode = Mode.Interval(6, IntervalUnit.HOUR),
    album = AlbumRef("travel", "旅行 2025", androidx.compose.ui.graphics.Color(0xFF16A34A), 328),
    display = DisplayMode.CROP,
    liveWallpaper = false,
    albumOptions = listOf(
        AlbumRef("mine",   "我的相册",   androidx.compose.ui.graphics.Color(0xFF2F6FEB), 1247),
        AlbumRef("travel", "旅行 2025", androidx.compose.ui.graphics.Color(0xFF16A34A), 328),
        AlbumRef("family", "家庭",      androidx.compose.ui.graphics.Color(0xFFF59E0B), 156),
        AlbumRef("work",   "工作",      androidx.compose.ui.graphics.Color(0xFF8B5CF6), 47),
        AlbumRef("fav",    "收藏",      androidx.compose.ui.graphics.Color(0xFFEF4444), 23),
    ),
    childrenOf = mapOf(
        "mine" to listOf(
            AlbumRef("mine-2025", "2025", androidx.compose.ui.graphics.Color(0xFF2F6FEB), 328),
            AlbumRef("mine-2024", "2024", androidx.compose.ui.graphics.Color(0xFF2F6FEB), 612),
        ),
        "family" to listOf(
            AlbumRef("family-baby", "宝宝成长", androidx.compose.ui.graphics.Color(0xFFEC4899), 89),
        ),
    ),
    sheet = WallpaperSheetState.Closed,
)
