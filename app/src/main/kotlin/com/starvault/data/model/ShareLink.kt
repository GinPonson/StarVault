package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 分享链接。Share 屏接收 fileId 后过滤出该文件的所有分享链接。
 *
 *  - url          完整分享 URL
 *  - accessCode   提取码（4-6 位）
 *  - expiresAt    null = 永久
 */
@Serializable
data class ShareLink(
    val fileId: String,
    val url: String,
    val accessCode: String,
    val expiresAt: Long? = null,
)
