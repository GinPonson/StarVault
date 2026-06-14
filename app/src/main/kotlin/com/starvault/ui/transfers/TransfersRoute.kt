package com.starvault.ui.transfers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.nav.Route

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 *  - 持有 [TransfersViewModel]（由 NavBackStackEntry scope 管理）
 *  - onSearch → TODO: 弹搜索
 *  - onMore   → TODO: 弹单条菜单
 *
 * @param nav  Nav 控制器（用于单条点击跳详情）
 * @param vm   注入 ViewModel
 */
@Composable
fun TransfersRoute(
    nav: NavHostController,
    vm: TransfersViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    TransfersScreen(
        state = state,
        onSearch = { /* TODO: T22+ 弹搜索 */ },
        onClear = vm::clearDone,
        onTab = vm::selectTab,
        onPauseAll = vm::pauseAll,
        onPause = vm::togglePause,
        onMore = { /* TODO: 弹菜单 */ },
        onRetry = vm::retry,
    )
}
