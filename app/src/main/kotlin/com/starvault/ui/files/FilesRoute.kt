package com.starvault.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.nav.Route

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * @param args  Route.Files(folderId: String?) — null → 根
 * @param nav   Nav 控制器
 * @param vm    注入 ViewModel
 */
@Composable
fun FilesRoute(
    args: Route.Files,
    nav: NavHostController,
    vm: FilesViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    FilesScreen(
        state = state,
        onSearch = { /* TODO: 弹搜索 */ },
        onTransfers = { nav.navigate(Route.Transfers) },
        onMore = { /* TODO: 弹文件更多菜单 */ },
        onNewFolder = { /* TODO: 弹新建文件夹 */ },
        onTypeClick = vm::selectType,
        onViewToggle = vm::changeViewMode,
        onSort = { /* TODO: 弹排序菜单 */ },
        onSelect = { e -> vm.toggleSelect(e.id) },
        onOpen = { e ->
            // VIDEO/IMAGE/AUDIO → Player；FOLDER → pushRoute；DOC/ZIP → 弹预览
            // Phase 1 stub
        },
        onCloseBulk = vm::clearSelection,
        onBulkAction = vm::bulk,
        onUpload = { /* TODO: 弹上传选择 */ },
    )
}
