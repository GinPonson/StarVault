package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 登录后 Profile 屏用。MVP 阶段字段对应 design/05-profile.html：
 *  - nickname     头像旁边的昵称
 *  - avatarUrl    Phase 1 不真正下载，用首字母 + 配色占位
 *  - vipLevel     0 = 普通，1+ = VIP
 *  - vipExpiresAt null = 永久 / 非 VIP
 *  - totalBytes / usedBytes 容量进度环
 */
@Serializable
data class User(
    val nickname: String,
    val avatarUrl: String? = null,
    val vipLevel: Int,
    val vipExpiresAt: Long? = null,
    val totalBytes: Long,
    val usedBytes: Long,
)
