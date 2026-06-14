package com.starvault.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Profile 屏 ViewModel — Phase 1 mock。
 *
 * 真实接入 115 后：
 *  - storage        ← /user/space
 *  - wallpaper      ← 读 SharedPreferences 同步
 *  - commonRows / settingRows ← 静态配置
 *
 * design body 里没有 user / vip 段，所以也不在 state 里。
 */
class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(mockState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /* ─────────────────── mock state ─────────────────── */

    private fun mockState(): ProfileUiState.Success {
        val storage = Storage(
            usedPct = 71,
            totalLabel = "1 TB",
            releaseDate = "2026/06/07 释放",
            breakdowns = listOf(
                Breakdown("视频",  Color(0xFF2F6FEB), "112.4 GB"),
                Breakdown("图片",  Color(0xFF9333EA), "48.2 GB"),
                Breakdown("文档",  Color(0xFF16A34A), "12.7 GB"),
                Breakdown("音频",  Color(0xFFEA580C), "38.5 GB"),
                Breakdown("其他",  Color(0xFFD4D4D4), "26.6 GB"),
            ),
            remainingGb = "761.6 GB",
            trashGb = "2.1 GB",
        )
        val wallpaper = Wallpaper(
            enabled = false,
            subText = "让相册成为会动的壁纸",
        )
        val commonRows = listOf(
            RowItem("↻", iconAccent = true,  label = "我的分享", rightText = "12 个进行中"),
            RowItem("🗑", iconAccent = false, label = "回收站",   rightText = "2.1 GB"),
            RowItem("▭", iconAccent = false, label = "设备管理", rightText = "3 台"),
        )
        val settingRows = listOf(
            RowItem("⛨", iconAccent = false, label = "隐私与安全"),
            RowItem("◫", iconAccent = false, label = "外观与主题", rightText = "跟随系统"),
            RowItem("?",  iconAccent = false, label = "帮助与反馈", rightBadge = "v6.2.1"),
        )
        return ProfileUiState.Success(
            storage = storage,
            wallpaper = wallpaper,
            commonRows = commonRows,
            settingRows = settingRows,
        )
    }
}
