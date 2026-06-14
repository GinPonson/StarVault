package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 文件/文件夹的统一抽象。Home / Files / Album / Player 四个屏都靠它。
 *
 *  - id          唯一标识
 *  - name        显示名
 *  - type        类型 (folder / video / image / doc / audio / zip / other)
 *  - sizeBytes   null 表示 folder（不展示大小）；非空时按 size 渲染
 *  - durationOrCount 复用字段：video=时长 (HH:MM:SS)、audio=时长、image=分辨率 (4032×3024)、folder=子项数 ("28 项")
 *  - mtime       Unix ms 修改时间
 *  - tag         可选标签（含 label + 5 色 color）
 *  - thumbnailUrl Phase 1 不真正下载，作为未来 Playwright 截屏占位
 */
@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val sizeBytes: Long? = null,
    val durationOrCount: String? = null,
    val mtime: Long,
    val tag: FileTag? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
enum class FileType { FOLDER, VIDEO, IMAGE, DOC, AUDIO, ZIP, OTHER }

@Serializable
enum class TagColor { TAG1, TAG2, TAG3, TAG4, TAG5 }

/**
 * 标签：与 design HTML 的 `<span class="tag t-X">label</span>` 一一对应。
 *
 *  - label  显示文本（如 "工作"/"影视"/"音乐"）
 *  - color  5 色之一，前景与 accent-soft 风格背景由 [color] 决定
 */
@Serializable
data class FileTag(val label: String, val color: TagColor)

