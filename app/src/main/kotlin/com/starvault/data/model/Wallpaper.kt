package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 单张壁纸 + 壁纸切换配置。
 *
 *  - category         分类（自然 / 城市 / 抽象...）
 *  - displayMode      填充策略：FILL_CROP（裁剪铺满）/ FIT_FULL（完整显示）/ CENTER（居中不缩放）
 *  - intervalSeconds  切换间隔
 */
@Serializable
data class Wallpaper(
    val id: String,
    val previewUrl: String,
    val category: String,
)

@Serializable
data class WallpaperConfig(
    val enabled: Boolean,
    val intervalSeconds: Int,
    val displayMode: DisplayMode,
    val categories: List<String>,
)

@Serializable
enum class DisplayMode { FILL_CROP, FIT_FULL, CENTER }
