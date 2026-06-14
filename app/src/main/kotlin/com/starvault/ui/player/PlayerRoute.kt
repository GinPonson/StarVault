package com.starvault.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starvault.nav.Route

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 *  - 持有 [PlayerViewModel]（由 NavBackStackEntry scope 管理）
 *  - 文件 id 由 [args] 携带：调用方从 Home / Files / Album 跳过来
 *  - 真实接入 115 后：onDownload / onTransfer / onShare / onAddTag 接到对应服务
 *  - onBack 由 NavHost 注入：popBackStack() 或 navigate(Route.Home)
 *
 * @param args          来自 NavBackStackEntry 的 Route.Player 反序列化结果
 * @param onBack        返回上一页
 * @param onRelated     相关推荐点击（→ 跳 Route.Player(fileId=related.id)）
 * @param vm            注入 ViewModel（Preview 时由测试侧覆写）
 */
@Composable
fun PlayerRoute(
    args: Route.Player,
    onBack: () -> Unit,
    onRelated: (String) -> Unit = {},
    vm: PlayerViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // 提前快照当前 speedChip 给 onSpeed 用，避免 delegated property 的 smart cast 失败
    val currentSpeed = (state as? PlayerUiState.Ready)?.speedChip ?: "1.0×"

    PlayerScreen(
        state = state,
        onBack = onBack,
        onTogglePlay = vm::togglePlay,
        onSeek = vm::seekTo,
        onSpeed = { vm.changeSpeed(if (currentSpeed == "1.0×") "1.5×" else "1.0×") },
        onDownload = { /* TODO: T31+ 接 115 download API */ },
        onTransfer = { /* TODO: T31+ 接 115 transfer API */ },
        onShare = { /* TODO: 跳 Route.Share(fileId) */ },
        onAddTag = { /* TODO: T31+ 弹 tag picker */ },
        onTagClick = vm::setTag,
        onRelated = { onRelated(it.id) },
    )
}

