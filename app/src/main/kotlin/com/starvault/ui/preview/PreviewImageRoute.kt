package com.starvault.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.starvault.nav.Route

/**
 * PreviewImage Route 入口（NavHost 唯一注入点）。
 *
 *  通过自定义 ViewModelFactory 把 [Route.PreviewImage.fileId] 传给 VM 构造参数，
 *  这是 Compose Navigation type-safe route + savedstate 的标准用法：
 *  ViewModel 不再单独 SavedStateHandle 解析（route 已在 args 里 type-safe 拆好）。
 *
 *  M5 CRUD:订阅 [PreviewImageViewModel.oneShotEvents],收到 [PreviewEvent.Deleted]
 *  → 调 [onBack] 回 Files。Rename / Move 不在 Route 层处理:
 *  - Rename:VM 内 state 已更新,UI 自然响应,无需 Route 介入
 *  - Move:VM 也 emit Deleted(文件已离开当前目录),同样走 onBack
 *
 *  @param args  Route.PreviewImage(fileId) — 来自 NavHost composable<Route.PreviewImage>
 *  @param onBack 返回 Files；NavHost 内部 popBackStack
 *  @param nav   Nav 控制器(用于 MoreMenu MOVE 跳 FolderPicker)
 */
@Composable
fun PreviewImageRoute(
    args: Route.PreviewImage,
    onBack: () -> Unit,
    nav: NavHostController,
) {
    val vm: PreviewImageViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PreviewImageViewModel(fileId = args.fileId) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val isStarred by vm.isStarred.collectAsStateWithLifecycle()
    // Application context for shareImage(FileProvider 拿 cacheDir + startActivity 都行;屏销毁不丢正在进行的下载)
    val appContext = LocalContext.current.applicationContext

    // 监听 FolderPicker 选完返回的 pickedCid(savedStateHandle 回传,见 FolderPickerRoute)
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("pickedCid", null)?.collect { pickedCid ->
            if (pickedCid != null) {
                vm.onMoveConfirmed(pickedCid)
                savedStateHandle["pickedCid"] = null
            }
        }
    }

    // 一次性事件订阅:VM emit Deleted → Route 调 onBack 回 Files
    LaunchedEffect(Unit) {
        vm.oneShotEvents.collect { event ->
            if (event is PreviewEvent.Deleted) onBack()
        }
    }

    PreviewImageScreen(
        state = state,
        isStarred = isStarred,
        onBack = onBack,
        onToggleStar = vm::toggleStar,
        onRename = vm::onRename,
        onDelete = vm::onDelete,
        onShare = { vm.onShare(appContext) },
        onDownload = vm::onDownload,
        onMove = {
            // excludeIds:仅自身 fid(MediaMetadata 不含 parentCid 字段,无法排除父目录)。
            // 自身排除已防自循环;用户移到原父目录不会出错(同位置 move 在 115 端 noop 即可)。
            val current = vm.state.value as? PreviewUiState.Success
            val exclude = listOfNotNull(current?.metadata?.fid)
            nav.navigate(Route.FolderPicker(excludeIds = exclude))
        },
    )
}