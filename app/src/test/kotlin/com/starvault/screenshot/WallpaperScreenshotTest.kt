package com.starvault.screenshot

import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.wallpaper.AlbumRef
import com.starvault.ui.wallpaper.DisplayMode
import com.starvault.ui.wallpaper.IntervalUnit
import com.starvault.ui.wallpaper.Mode
import com.starvault.ui.wallpaper.WallpaperScreen
import com.starvault.ui.wallpaper.WallpaperSheetState
import com.starvault.ui.wallpaper.WallpaperUiState
import org.junit.Rule
import org.junit.Test

/**
 * Wallpaper 屏 Paparazzi 回归基线 — 与 design/08-wallpaper.html 对齐。
 *
 *  - off   : 引擎关闭态（仅 SwitchCard 可见）
 *  - on    : 引擎开启态（4 个 section + FooterBar）
 *  - mode  : 切换频率 sheet 打开
 */
class WallpaperScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private val albumOptions = listOf(
        AlbumRef("mine", "我的相册", Color(0xFF2F6FEB), 1247),
        AlbumRef("travel", "旅行 2025", Color(0xFF16A34A), 328),
        AlbumRef("family", "家庭", Color(0xFFF59E0B), 156),
        AlbumRef("work", "工作", Color(0xFF8B5CF6), 47),
        AlbumRef("fav", "收藏", Color(0xFFEF4444), 23),
    )

    private val childrenOf = mapOf(
        "mine" to listOf(
            AlbumRef("mine-2025", "2025", Color(0xFF2F6FEB), 328),
            AlbumRef("mine-2024", "2024", Color(0xFF2F6FEB), 612),
        ),
        "family" to listOf(
            AlbumRef("family-baby", "宝宝成长", Color(0xFFEC4899), 89),
        ),
    )

    private fun success(enabled: Boolean, sheet: WallpaperSheetState = WallpaperSheetState.Closed) =
        WallpaperUiState.Success(
            enabled = enabled,
            mode = Mode.Interval(6, IntervalUnit.HOUR),
            album = albumOptions[1], // travel
            display = DisplayMode.CROP,
            liveWallpaper = false,
            albumOptions = albumOptions,
            childrenOf = childrenOf,
            sheet = sheet,
        )

    @Test fun wallpaper_off() = paparazzi.snapshot {
        StarVaultTheme {
            WallpaperScreen(state = success(enabled = false))
        }
    }

    @Test fun wallpaper_on() = paparazzi.snapshot {
        StarVaultTheme {
            WallpaperScreen(state = success(enabled = true))
        }
    }

    @Test fun wallpaper_modeSheet() = paparazzi.snapshot {
        StarVaultTheme {
            WallpaperScreen(state = success(enabled = true, sheet = WallpaperSheetState.ModePicker))
        }
    }
}