package com.starvault.ui.share

import com.starvault.data.model.FileItem
import com.starvault.data.model.FileType
import com.starvault.data.model.ShareLink

/**
 * Share 屏 UiState（对应 design/03-share.html 的链接分享 tab）。
 *
 *  - Loading   : 拉取文件元数据中（背景 row 模糊已显示）
 *  - Ready     : 完整渲染，activeTab 决定显示哪个 tab 内容
 *  - Error     : 文件不存在 / 无权访问(Phase 1 mock 不再使用,改走 ToastBus 错误提示)
 *
 * 三个 tab（与 HTML `.tab` 一一对应）：
 *  - Link    : 链接分享（默认，显示 QR + link result + 3 toggle）
 *  - Save    : 转存到我的
 *  - Send    : 发送给…
 *
 * 真实接入 115 后：tabs 三态共享一个 Ready，把 activeTab 拉成顶级 state，
 * 三个 tab 内容各自独立函数渲染。
 */
sealed interface ShareUiState {

    val file: FileItem?
    val activeTab: ShareTab

    data class Loading(
        override val activeTab: ShareTab = ShareTab.Link,
    ) : ShareUiState {
        override val file: FileItem? = null
    }

    data class Ready(
        override val file: FileItem,
        override val activeTab: ShareTab = ShareTab.Link,
        val accessType: String = "有提取码 · 任何人",
        val accessCode: String = "8K3F",
        val expiresInDays: Int = 7,
        val forbidTransfer: Boolean = false,
        val vipOnly: Boolean = true,
        val loginRequired: Boolean = true,
        val link: ShareLink? = null,
        val copiedCount: Int = 3,
    ) : ShareUiState

}

enum class ShareTab(val label: String) {
    Link("链接分享"),
    Save("转存到我的"),
    Send("发送给…"),
}

/** 设计 HTML 用的视频 mock（与 Player 屏共享 h-02 标识）。*/
internal fun mockShareFile() = FileItem(
    id = "h-02",
    name = "Final.Destination.2026.1080p.mkv",
    type = FileType.VIDEO,
    sizeBytes = 2_340_234_240L,
    durationOrCount = "1:42:08",
    mtime = 1_730_000_000_000L,
)
