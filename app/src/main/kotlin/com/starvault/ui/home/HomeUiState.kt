package com.starvault.ui.home

import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor

/**
 * Home 屏 UiState（对应 design/01-home.html 的 4 段结构）。
 *
 *  - Loading       首次进入：占位空列表，避免 row 闪一下空状态
 *  - Success       正常态：files 列表 + 当前 tag 过滤器（null = 全部）
 *  - Error         取数据失败（Phase 1 mock 不可达，预留）
 *
 * 排序策略：固定按 mtime desc 排（与 HTML "排序：最近 ▾" 默认一致），
 * 真实接入 115 后可由 ViewModel 暴露 sortKey 字段控制。
 */
sealed interface HomeUiState {

    val files: List<FileItem>
    val activeTag: FileTag?
    /**
     * fileId → 友好相对时间字符串（"2 小时前" / "今天 09:12" / "昨天 23:14"）。
     * 与 HTML 1:1；硬编码以保证 Paparazzi 截图稳定，避免 mtime 计算漂移。
     */
    val relTimes: Map<String, String>

    data class Loading(
        override val activeTag: FileTag? = null,
        override val relTimes: Map<String, String> = emptyMap(),
    ) : HomeUiState {
        override val files: List<FileItem> = emptyList()
    }

    data class Success(
        override val files: List<FileItem>,
        override val activeTag: FileTag? = null,
        override val relTimes: Map<String, String> = emptyMap(),
    ) : HomeUiState

    data class Error(
        val message: String,
        override val activeTag: FileTag? = null,
        override val relTimes: Map<String, String> = emptyMap(),
    ) : HomeUiState {
        override val files: List<FileItem> = emptyList()
    }
}

/**
 * 顶栏 tag-strip 上的 6 个候选标签（与 HTML 完全一致）。
 *
 *  - `null`            = "全部"（active 时 fg 底白字 + check icon）
 *  - 5 个具名 tag      = tag 列表里的工作/学习/影视/资料/音乐
 *  - 颜色一一对应 TagColor.TAG1..5
 *  - 用 enum class 而非 data class：避免与 FileTag 嵌套、也避免反序列化歧义
 */
enum class HomeQuickTag(val label: String, val color: TagColor) {
    WORK("工作",  TagColor.TAG1),
    STUDY("学习", TagColor.TAG2),
    MEDIA("影视", TagColor.TAG3),
    REF("资料",   TagColor.TAG4),
    MUSIC("音乐", TagColor.TAG5),
    ;

    fun toFileTag(): FileTag = FileTag(label = label, color = color)
}
