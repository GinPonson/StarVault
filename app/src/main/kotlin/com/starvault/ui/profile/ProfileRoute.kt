package com.starvault.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * @param nav Nav 控制器（点击 row 后跳详情 / 跳到 Wallpaper / 退出回 Login）
 * @param vm  注入 ViewModel
 */
@Composable
fun ProfileRoute(
    nav: NavHostController,
    vm: ProfileViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onWallet = { /* TODO: 弹钱包面板 */ },
        onSettings = { /* TODO: 跳设置 */ },
        onUpgrade = { /* TODO: 跳扩容 */ },
        onRow = { /* TODO: 按 row.label 决定 */ },
        onWallpaper = { nav.navigate(com.starvault.nav.Route.Wallpaper) },
        onLogout = { /* TODO: 清 token + nav 回 Login */ },
    )
}
