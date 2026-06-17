package com.starvault.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Profile 屏 UiState（严格对应 design/05-profile.html 的 body 段）。
 *
 * design 里的 6 段（按出现顺序）：
 *   1. header           我的 + 钱包/设置
 *   2. storage-big      云端空间 + 环 + breakdown
 *   3. wp-card          壁纸引擎（单行）
 *   4. section          我的分享 / 回收站 / 设备管理
 *   5. section          隐私与安全 / 外观与主题 / 帮助与反馈
 *   6. logout           退出登录
 *
 * 注意：design HTML 里 **没有** 头像 UserCard，也 **没有** VIP 卡 —— 那两个 card 的
 * CSS（.avatar / .user / .vip）虽然定义在 <style>，但 body 里从未引用。先前实现的
 * 两段是凭空加的，需移除。
 */
sealed interface ProfileUiState {

    data class Loading(
        val placeholder: Unit = Unit,
    ) : ProfileUiState

    data class Success(
        val storage: Storage,
        val wallpaper: Wallpaper,
        val commonRows: List<RowItem>,
        val settingRows: List<RowItem>,
    ) : ProfileUiState

    data class Error(
        val message: String,
    ) : ProfileUiState
}

/* ───────────────────── 子模型 ───────────────────── */

/** 存储大卡：环 + 5 条 breakdown + 剩余。
 *
 *  - usedPct / totalLabel / remainingGb / trashGb 来自 webapi /user/space_summury（真）
 *  - releaseDate 仍是写死字符串（115 不返回「释放日」）
 *  - breakdowns 来自 design mock（115 不返回 5 类分布），用 [breakdownsIsMock] 标识
 *  - userName 用于标题 "云端空间 — Alice"，为空时仅显示 "云端空间"
 */
data class Storage(
    val usedPct: Int,              // 71
    val totalLabel: String,        // "1 TB"
    val releaseDate: String,       // "2026/06/07 释放"
    val breakdowns: List<Breakdown>,
    val remainingGb: String,       // "761.6 GB"
    val trashGb: String,           // "2.1 GB"
    val userName: String = "",         // "" → StorageCard 标题仅显示 "云端空间"
    val breakdownsIsMock: Boolean = true,  // true → 5 行 breakdown 是 mock 假数据
)

/** 5 行 breakdown 之一（视频 / 图片 / 文档 / 音频 / 其他）。 */
data class Breakdown(
    val label: String,             // "视频"
    val swatch: Color,             // 8x8 色块
    val sizeText: String,          // "112.4 GB"
)

/** 壁纸引擎卡（5 个字段支持两态：off 单行 / on 双行详情）。 */
data class Wallpaper(
    val enabled: Boolean,          // false: 灰 badge / on: 蓝 badge
    val subText: String,           // 关闭态："让相册成为会动的壁纸"
                                    // 启用态："mode · display · 动态壁纸:开/关"
    val enabledAlbum: String? = null,  // 启用时显示的相册名 "旅行 2025"
)

/** 通用 row：图标 + 标题 + 右侧文本 + 角标。 */
data class RowItem(
    val icon: ImageVector,         // 来自 design 的 SVG icon
    val iconAccent: Boolean,       // true → accent-10% 背景的图标
    val label: String,             // "我的分享"
    val rightText: String? = null, // "12 个进行中"
    val rightBadge: String? = null,// "v6.2.1"
)
