package com.starvault.ui.player

import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.TagColor

/**
 * Player 屏 UiState（对应 design/02-player.html 的播放态机）。
 *
 *  - Loading   : 拉取视频元数据中
 *  - Ready     : 视频元数据 + 进度 + 相关推荐齐全
 *  - Error     : 拉取失败 / 视频不存在
 *
 * 进度用 0..1 浮点表示（避免 [Long] ms 容易溢出；UI 层转成 "32:14 / 1:42:08"）。
 * 标签：5 个 FileTag（影视/4K源/未加密…），外加 "添加标签" 入口（用 isAddingTag=true 区分）。
 */
sealed interface PlayerUiState {

    val file: FileItem?
    val progress: Float       // 0f..1f
    val isPlaying: Boolean
    val tags: List<FileTag>
    val isAddingTag: Boolean

    data class Loading(
        override val file: FileItem? = null,
        override val progress: Float = 0f,
        override val isPlaying: Boolean = false,
        override val tags: List<FileTag> = emptyList(),
        override val isAddingTag: Boolean = true,
    ) : PlayerUiState

    data class Ready(
        override val file: FileItem,
        override val progress: Float = 0f,
        override val isPlaying: Boolean = false,
        override val tags: List<FileTag> = emptyList(),
        override val isAddingTag: Boolean = true,
        val position: String = "00:00",
        val duration: String = "00:00",
        val resolution: String = "1080P",
        val codec: String = "H.265",
        val savedAt: String = "2026-04-12",
        val path: String = "/影视/科幻/",
        val uploader: String = "我",
        val sha1: String = "9f2c…b4e1",
        val downloadCount: Int = 0,
        val related: List<RelatedVideo> = emptyList(),
        val qualityChip: String = "1080P",
        val speedChip: String = "1.0×",
        val audioChip: String = "原声",
        val bufferedMb: String = "0.0",
    ) : PlayerUiState

    data class Error(
        val message: String,
        override val file: FileItem? = null,
        override val progress: Float = 0f,
        override val isPlaying: Boolean = false,
        override val tags: List<FileTag> = emptyList(),
        override val isAddingTag: Boolean = true,
    ) : PlayerUiState
}

/**
 * 相关推荐的一条视频（与设计 HTML `.related .row` 一一对应）。
 *
 *  - thumbColorHex 与 design HTML `.related .thumb { background: linear-gradient(...) }` 对齐
 *  - duration 显示在右下角（如 "1:48:02"）
 */
data class RelatedVideo(
    val id: String,
    val name: String,
    val sizeText: String,
    val dateText: String,
    val durationText: String,
    val thumbColorHex: Long,
)

/** 5 个预设的"添加标签"内容（与设计 HTML tag-row 一一对应：影视/4K源/未加密/添加标签）。*/
object PlayerDefaultTags {
    fun preset(): List<FileTag> = listOf(
        FileTag("影视", TagColor.TAG3),
        FileTag("4K 源", TagColor.TAG4),
        FileTag("未加密", TagColor.TAG4),
    )
}
