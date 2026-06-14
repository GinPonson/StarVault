package com.starvault.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starvault.nav.Route

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 *  - 持有 [ShareViewModel]（由 NavBackStackEntry scope 管理）
 *  - 文件 id 由 [args] 携带：从 Player / Files 跳过来
 *  - onBack → popBackStack
 *  - 真实接入 115 后：onCta / onRegenCode / onCopy 接到 API
 *
 * @param args     来自 NavBackStackEntry 的 Route.Share
 * @param onBack   返回回调（NavHost 注入 = popBackStack）
 * @param vm       注入 ViewModel（Preview 时由测试侧覆写）
 */
@Composable
fun ShareRoute(
    args: Route.Share,
    onBack: () -> Unit,
    vm: ShareViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ShareScreen(
        state = state,
        onClose = onBack,
        onTab = vm::selectTab,
        onAccessType = { /* TODO: 弹 access type 选择器 */ },
        onRegenCode = vm::regenerateCode,
        onExpires = { /* TODO: 弹过期时间选择器 */ },
        onForbidTransfer = vm::toggleForbidTransfer,
        onVipOnly = vm::toggleVipOnly,
        onLoginRequired = vm::toggleLoginRequired,
        onCopy = { /* TODO: 复制到剪贴板 + 计数 */ },
        onCta = { /* TODO: 调 115 transfer API */ },
    )
}
