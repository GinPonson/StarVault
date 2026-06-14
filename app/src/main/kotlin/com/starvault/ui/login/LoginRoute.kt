package com.starvault.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 *  - 持有 [LoginViewModel]（由 NavBackStackEntry scope 管理）
 *  - 监听 state，当出现 [LoginUiState.LoggedIn] 时一次性触发 [onLoggedIn] 跳 Home
 *  - 把 UI 状态与命令委托给纯 UI 的 [LoginScreen]，方便 Preview / Paparazzi 直渲
 *
 * @param onLoggedIn 登录成功回调；NavHost 中调用 `nav.navigate(Route.Home) { popUpTo(Login) }`
 * @param vm         注入 ViewModel（Preview 时由测试侧覆写）
 */
@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // LaunchedEffect key = state，仅在状态变成 LoggedIn 这一刻执行一次
    // 避免重组时反复触发导航
    LaunchedEffect(state) {
        if (state is LoginUiState.LoggedIn) onLoggedIn()
    }

    LoginScreen(
        state = state,
        onScanClick = vm::simulateScan,
        onRefresh   = vm::refresh,
    )
}
