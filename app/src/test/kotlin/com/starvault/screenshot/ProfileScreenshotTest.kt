package com.starvault.screenshot

import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.profile.Breakdown
import com.starvault.ui.profile.ProfileScreen
import com.starvault.ui.profile.ProfileUiState
import com.starvault.ui.profile.RowItem
import com.starvault.ui.profile.Storage
import com.starvault.ui.profile.Wallpaper
import org.junit.Rule
import org.junit.Test

/**
 * Profile 屏 Paparazzi 回归基线 — 与 design/05-profile.html 对齐（仅 6 段，无 avatar/VIP）。
 *
 *  - ready  : 默认主态（71% 存储环 + 5 行 breakdown + 壁纸引擎 + 2 个 section + 退出登录）
 *  - error  : 用户信息拉取失败
 */
class ProfileScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private val mockSuccess = ProfileUiState.Success(
        storage = Storage(
            usedPct = 71,
            totalLabel = "1 TB",
            breakdowns = listOf(
                Breakdown("视频", Color(0xFF2F6FEB), "112.4 GB"),
                Breakdown("图片", Color(0xFF9333EA), "48.2 GB"),
                Breakdown("文档", Color(0xFF16A34A), "12.7 GB"),
                Breakdown("音频", Color(0xFFEA580C), "38.5 GB"),
                Breakdown("其他", Color(0xFFD4D4D4), "26.6 GB"),
            ),
            remainingGb = "761.6 GB",
            trashGb = "2.1 GB",
        ),
        wallpaper = Wallpaper(
            enabled = false,
            subText = "让相册成为会动的壁纸",
        ),
        commonRows = listOf(
            RowItem(Icons.Refresh,  iconAccent = true,  label = "我的分享", rightText = "12 个进行中"),
            RowItem(Icons.Trash,    iconAccent = false, label = "回收站",   rightText = "2.1 GB"),
            RowItem(Icons.Device,   iconAccent = false, label = "设备管理", rightText = "3 台"),
        ),
        settingRows = listOf(
            RowItem(Icons.Privacy,    iconAccent = false, label = "隐私与安全"),
            RowItem(Icons.Appearance, iconAccent = false, label = "外观与主题", rightText = "跟随系统"),
            RowItem(Icons.Help,       iconAccent = false, label = "帮助与反馈", rightBadge = "v6.2.1"),
        ),
    )

    @Test fun profile_ready() = paparazzi.snapshot {
        StarVaultTheme {
            ProfileScreen(state = mockSuccess)
        }
    }
}