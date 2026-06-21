package com.starvault.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.data.model.FileType
import com.starvault.nav.Route

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * @param args  Route.Files(folderId: String?) — null → 根 "0"
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

    // 启动时按 args 切目录（null = 根 "0"，VM.init 已默认加载）
    LaunchedEffect(args.folderId) {
        if (args.folderId != null) vm.setFolder(args.folderId)
    }

    FilesScreen(
        state = state,
        onBack = {
            // 先尝试目录层级回退；栈已到根时再 popBackStack 退出 Files
            if (!vm.backFolder()) nav.popBackStack()
        },
        onSearch = { /* TODO: 弹搜索 */ },
        onTransfers = { nav.navigate(Route.Transfers) },
        onMore = { /* TODO: 弹文件更多菜单 */ },
        onNewFolder = { /* TODO: 弹新建文件夹 */ },
        onTypeClick = vm::selectType,
        onViewToggle = vm::changeViewMode,
        onSort = { /* TODO: 弹排序菜单 */ },
        onSelect = { e -> vm.toggleSelect(e.id) },
        onOpen = { e ->
            // 文件夹 → 切子目录
            // IMAGE   → PreviewImage
            // VIDEO   → PreviewVideo
            // AUDIO / DOC / ZIP / OTHER → 当前 phase 不接预览，noop（后续 phase）
            when {
                e.isFolder -> vm.setFolder(e.id, e.name)
                e.type == FileType.IMAGE -> nav.navigate(Route.PreviewImage(e.id))
                e.type == FileType.VIDEO -> nav.navigate(Route.PreviewVideo(e.id))
                else -> { /* TODO: 详情页 / 下载 */ }
            }
        },
        onCrumbClick = { index -> vm.popToFolder(index) },
        onCloseBulk = vm::clearSelection,
        onBulkAction = vm::bulk,
        onUpload = { /* TODO: 弹上传选择 */ },
        onLoadMore = vm::loadMore,
    )
}
