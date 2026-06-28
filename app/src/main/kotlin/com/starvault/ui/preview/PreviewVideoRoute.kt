package com.starvault.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.starvault.core.ToastBus
import com.starvault.nav.Route

/**
 * PreviewVideo Route 入口（NavHost 唯一注入点）。
 *
 *  与 PreviewImageRoute 几乎对称：把 [Route.PreviewVideo.fileId] / [parentCid] 传给 VM。
 *  Compose Navigation 的 type-safe route + savedstate 解出 args，
 *  ViewModel 不需要自己 SavedStateHandle.get()。
 *
 *  上一集/下一集:Route.PreviewVideo 接收 [nav] 控制器,Siblings 状态从 VM 读;
 *  切到 fid 时 [Route.PreviewVideo.parentCid] 一并带上(同一父目录),
 *  这样 VM 重新拉兄弟时仍然是同一集合,prev/next 一致。
 *
 *  M5 CRUD:订阅 [PreviewVideoViewModel.oneShotEvents],收到 [PreviewEvent.Deleted]
 *  → 调 [onBack] 回 Files;MoreMenu MOVE 跳 FolderPicker 选完回传 pickedCid 后调
 *  [PreviewVideoViewModel.onMoveConfirmed](同 FilesRoute 模式)。
 *
 *  @param args  Route.PreviewVideo(fileId, parentCid?)
 *  @param onBack 返回 Files；NavHost 内部 popBackStack
 *  @param nav   Nav 控制器（用于上一集/下一集兄弟跳转 + FolderPicker）
 */
@Composable
fun PreviewVideoRoute(
    args: Route.PreviewVideo,
    onBack: () -> Unit,
    nav: NavHostController,
) {
    val vm: PreviewVideoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PreviewVideoViewModel(
                    fileId = args.fileId,
                    parentCid = args.parentCid,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val siblings by vm.siblings.collectAsStateWithLifecycle()
    val isStarred by vm.isStarred.collectAsStateWithLifecycle()
    val playlist by vm.playlist.collectAsStateWithLifecycle()

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

    PreviewVideoScreen(
        state = state,
        siblings = siblings,
        isStarred = isStarred,
        onBack = onBack,
        onSelectQuality = vm::selectQuality,
        onPrev = { siblings.prevId?.let { nav.navigate(Route.PreviewVideo(it, args.parentCid)) } },
        onToggleStar = vm::toggleStar,
        onSavePosition = vm::savePosition,
        playlist = playlist,
        onSelectFromPlaylist = { fid -> nav.navigate(Route.PreviewVideo(fid, args.parentCid)) },
        onRename = vm::onRename,
        onDelete = vm::onDelete,
        onShare = { ToastBus.info("仅图片可分享") },
        onMove = {
            // excludeIds:自身 fid + parentCid(若有);FolderPicker 行点击时若 id ∈ excludeIds → noop
            val current = vm.state.value as? PreviewUiState.Success
            val exclude = listOfNotNull(
                current?.metadata?.fid,
                args.parentCid?.takeIf { it.isNotBlank() },
            )
            nav.navigate(Route.FolderPicker(excludeIds = exclude))
        },
    )
}