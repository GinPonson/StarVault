package com.starvault.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.BuildConfig

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * @param nav Nav 控制器（点击 row 后跳详情 / 跳到 Wallpaper）
 * @param vm  注入 ViewModel
 *
 * 退出登录：onLogout → vm.onSignOut()（VM 内部 viewModelScope.launch）→ DataStore 清 token
 * → authState 切 Unauthenticated → NavHost 自动从 Home 栈 pop 到 Login（**不**在 Route 手动 nav）。
 * 失败时（极少见 — DataStore.clear 抛 IOException）通过全局 [com.starvault.core.ToastBus] 投递,
 * 由 StarVaultApp 顶层的 ToastHost 渲染为 Snackbar。
 */
@Composable
fun ProfileRoute(
    nav: NavHostController,
    vm: ProfileViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileScreen(
            state = state,
            onSettings = { /* TODO: 跳设置 */ },
            onRow = { /* TODO: 按 row.label 决定 */ },
            onWallpaper = { nav.navigate(com.starvault.nav.Route.Wallpaper) },
            onLogout = { vm.onSignOut() },
        )
        // DEBUG 入口（仅 debug 包可见）：跳到 ThumbStateLab 看缩略图四态
        if (BuildConfig.DEBUG) {
            androidx.compose.material3.Text(
                text = "DEBUG → 缩略图四态",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 90.dp)
                    .clickable(
                        onClick = { nav.navigate(com.starvault.nav.Route.ThumbLab) },
                    ),
            )
        }
    }
}
