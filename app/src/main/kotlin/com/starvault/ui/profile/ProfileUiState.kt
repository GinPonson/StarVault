package com.starvault.ui.profile

import androidx.compose.ui.graphics.Color

/**
 * Profile 屏 UiState（对应 design/05-profile.html）。
 *
 *  - Loading : 占位
 *  - Success : 用户信息 + 存储用量 + VIP 状态 + 壁纸引擎 + 2 个功能区 + 退出
 *  - Error   : 拉取失败
 */
sealed interface ProfileUiState {

    data class Loading(
        val placeholder: Unit = Unit,
    ) : ProfileUiState

    data class Success(
        val user: User,
        val storage: Storage,
        val vip: Vip,
        val wallpaper: Wallpaper,
        val commonRows: List<RowItem>,
        val settingRows: List<RowItem>,
    ) : ProfileUiState

    data class Error(
        val message: String,
    ) : ProfileUiState
}

/* ───────────────────── 子模型 ───────────────────── */

/** 顶部用户卡。 */
data class User(
    val avatarInitial: String,     // 头像中央的字母（"H"）
    val name: String,              // "何湘湘"
    val isVip: Boolean = true,     // true 时显示 VIP 徽章
    val id: String,                // UID：UID_8945721
)

/** 存储大卡：环 + 5 条 breakdown + 剩余。 */
data class Storage(
    val usedPct: Int,              // 71
    val totalLabel: String,        // "1 TB"
    val releaseDate: String,       // "2026/06/07 释放"
    val breakdowns: List<Breakdown>,
    val remainingGb: String,       // "761.6 GB"
    val trashGb: String,           // "2.1 GB"
)

/** 5 行 breakdown 之一（视频 / 图片 / 文档 / 音频 / 其他）。 */
data class Breakdown(
    val label: String,             // "视频"
    val swatch: Color,             // 8x8 色块
    val sizeText: String,          // "112.4 GB"
)

/** VIP 卡。 */
data class Vip(
    val tierCode: String,          // "L4"
    val tierName: String,          // "钻石会员"
    val expireText: String,        // "2027/03/15 到期"
    val perks: List<String>,       // 3 个 perk 标签
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
    val iconGlyph: String,         // unicode 字符
    val iconAccent: Boolean,       // true → accent-10% 背景的图标
    val label: String,             // "我的分享"
    val rightText: String? = null, // "12 个进行中"
    val rightBadge: String? = null,// "v6.2.1"
)
