package com.starvault.nav

import kotlinx.serialization.Serializable

/**
 * 类型安全的 Compose Navigation 路由。
 *
 *  - data object    不带参的 tab / 栈页（Login / Home / Transfers / Profile / Album / Wallpaper）
 *  - data class     带参的栈页（Player.fileId / Share.fileId / Files.folderId?）
 *
 *  Phase 1 的 Album / Wallpaper / Files 都在同一个 com.starvault.ui.<feature> 包里，
 *  T13–T21 逐屏实装；T10 阶段只占位。
 */
sealed interface Route {
    @Serializable data object  Login                                              : Route
    @Serializable data object  Home                                               : Route
    @Serializable data class   Player(val fileId: String)                          : Route
    @Serializable data class   Share(val fileId: String)                           : Route
    @Serializable data object  Transfers                                          : Route
    @Serializable data object  Profile                                            : Route
    @Serializable data class   Files(val folderId: String? = null)                : Route
    @Serializable data class   PreviewImage(val fileId: String)                   : Route
    /**
     * 视频预览。
     *
     *  - [fileId]  : 115 文件 id
     *  - [parentCid]: 父目录 cid,用于上一集/下一集兄弟文件导航;
     *                null = 入口不传父目录(例如 Search 屏直跳),VM 不拉 siblings,
     *                上一集/下一集按钮降级为 noop + ToastBus
     */
    @Serializable data class   PreviewVideo(
        val fileId: String,
        val parentCid: String? = null,
    )                                                                          : Route
    /**
     * 音频预览(mp3/flac/wav 等,走 115 downurl 5min 签名直链 + ExoPlayer ProgressiveMediaSource)。
     *
     *  - [fileId]  : 115 文件 id
     *  - [parentCid]: 父目录 cid,用于上一首/下一首兄弟文件导航;null = 单首,按钮降级为 noop
     */
    @Serializable data class   PreviewAudio(
        val fileId: String,
        val parentCid: String? = null,
    )                                                                          : Route
    @Serializable data object  Album                                              : Route
    @Serializable data object  Wallpaper                                          : Route
    @Serializable data object  ThumbLab                                           : Route

    /**
     * 搜索屏（Files 屏的搜索入口跳转）。
     *
     * @param initialQuery 初始关键词（默认空 = 用户自己输入）。
     *                     留出 deep link 扩展点：未来可从 Home / Album 屏带初始词跳进来。
     */
    @Serializable data class   Search(val initialQuery: String = "")                : Route

    /**
     * 文件夹选择器（Files BulkBar MOVE 入口 + Preview 屏单文件 MOVE 入口）。
     *
     *  复用 FilesScreen 渲染目录树,用户点行 = 选中目标目录并返回;通过
     *  [androidx.navigation.NavController.previousBackStackEntry].savedStateHandle["pickedCid"]
     *  传回调用方(MOVE 时调用方读出后调 [com.starvault.data.repository.FilesRepository.moveFiles])。
     *
     * @param excludeIds 不允许选中的目录 cid 集合(防止移到自身/祖先造成自循环):
     *                   Files MOVE 传 currentCid + selectedIds(folder 行);
     *                   Preview MOVE 传 current.metadata.parentId + current.metadata.fid。
     *                   UI 层做行点击前过滤(若 id ∈ excludeIds 则 noop + ToastBus)。
     */
    @Serializable data class   FolderPicker(val excludeIds: List<String> = emptyList()) : Route
}
