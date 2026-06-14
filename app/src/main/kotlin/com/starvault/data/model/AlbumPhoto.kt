package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 相册照片。Album 屏用 LazyVerticalStaggeredGrid 排版，
 * width/height 给定以还原 design/07-album.html 的瀑布流。
 *
 *  - isFavorite  顶部 "★ Favorites" 筛选的依据
 */
@Serializable
data class AlbumPhoto(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val isFavorite: Boolean = false,
)
