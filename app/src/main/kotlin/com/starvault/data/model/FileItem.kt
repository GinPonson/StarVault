package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 文件/文件夹的统一抽象。Files / Album / Player 三个屏都靠它。
 *
 *  - id          唯一标识
 *  - name        显示名
 *  - type        类型（folder / video / image / doc / audio / other）
 *  - sizeBytes   null 表示 folder（不展示大小）
 *  - mtime       Unix ms
 *  - tag         可选 5 色 tag，对应 StarVaultColors.tag1..tag5
 *  - thumbnailUrl Phase 1 不真正下载，作为未来 Playwright 截屏占位
 */
@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val sizeBytes: Long? = null,
    val mtime: Long,
    val tag: TagColor? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
enum class FileType { FOLDER, VIDEO, IMAGE, DOC, AUDIO, OTHER }

@Serializable
enum class TagColor { TAG1, TAG2, TAG3, TAG4, TAG5 }
