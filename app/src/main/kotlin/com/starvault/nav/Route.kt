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
    @Serializable data object  Album                                              : Route
    @Serializable data object  Wallpaper                                          : Route
}
