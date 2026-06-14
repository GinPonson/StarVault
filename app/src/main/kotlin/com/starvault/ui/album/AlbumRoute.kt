package com.starvault.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * @param nav Nav 控制器（点击 photo → 跳 Player）
 * @param vm  注入 ViewModel
 */
@Composable
fun AlbumRoute(
    nav: NavHostController,
    vm: AlbumViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    AlbumScreen(
        state = state,
        onSearch = { /* TODO: 弹搜索 */ },
        onCamera = { /* TODO: 打开相机 */ },
        onMore = { /* TODO: 弹相册更多菜单 */ },
        onOpenSheet = vm::openSheet,
        onSubTab = vm::selectSubTab,
        onSeeAll = { /* TODO: 跳相册全量页 */ },
        onPhotoClick = { p ->
            if (p.kind == PhotoKind.VIDEO) {
                nav.navigate(com.starvault.nav.Route.Player("album-${p.id}"))
            } else {
                nav.navigate(com.starvault.nav.Route.Player("album-${p.id}"))
            }
        },
        onSelectAlbum = vm::selectFromSheet,
        onCloseSheet = vm::closeSheet,
        onNewFolder = { /* TODO: 弹新建相册对话框 */ },
    )
}
