package com.starvault.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme

/**
 * Profile 屏 Preview — 2 个 case：
 *  1. Default  : 完整我的屏（未启用壁纸引擎 / 71% 存储环 / 6 段标准布局 / 退出登录）
 *  2. Wallpaper: 同上，但壁纸引擎切换为"启用中"态（带 album 名 + meta 行）
 */
@Preview(name = "Profile/Default", showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun ProfileDefaultPreview() = StarVaultTheme {
    ProfileScreen(state = profilePreviewSuccess())
}

@Preview(name = "Profile/Wallpaper", showBackground = true, widthDp = 412, heightDp = 1380)
@Composable
fun ProfileWallpaperOnPreview() = StarVaultTheme {
    ProfileScreen(state = profilePreviewSuccess().copy(
        wallpaper = Wallpaper(
            enabled = true,
            subText = "每次解锁 · 居中裁剪 · 动态壁纸:开",
            enabledAlbum = "旅行 2025",
        ),
    ))
}

internal fun profilePreviewSuccess(): ProfileUiState.Success = ProfileUiState.Success(
    storage = Storage(
        usedPct = 71,
        totalLabel = "1 TB",
        breakdowns = listOf(
            Breakdown("视频", androidx.compose.ui.graphics.Color(0xFF2F6FEB), "112.4 GB"),
            Breakdown("图片", androidx.compose.ui.graphics.Color(0xFF9333EA), "48.2 GB"),
            Breakdown("文档", androidx.compose.ui.graphics.Color(0xFF16A34A), "12.7 GB"),
            Breakdown("音频", androidx.compose.ui.graphics.Color(0xFFEA580C), "38.5 GB"),
            Breakdown("其他", androidx.compose.ui.graphics.Color(0xFFD4D4D4), "26.6 GB"),
        ),
        remainingGb = "761.6 GB",
        trashGb = "2.1 GB",
    ),
    wallpaper = Wallpaper(enabled = false, subText = "让相册成为会动的壁纸"),
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
