package com.starvault.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.starvault.nav.Route

/**
 * PreviewAudio Route 入口(NavHost 唯一注入点)。
 *
 *  与 PreviewVideoRoute 几乎对称: 把 [Route.PreviewAudio.fileId] / [parentCid] 传给 VM。
 *  Compose Navigation 的 type-safe route + savedstate 解出 args,
 *  ViewModel 不需要自己 SavedStateHandle.get()。
 *
 *  上一首/下一首: Route.PreviewAudio 接收 [nav] 控制器, Siblings 状态从 VM 读;
 *  切到 fid 时 [Route.PreviewAudio.parentCid] 一并带上(同一父目录),
 *  这样 VM 重新拉兄弟时仍然是同一集合, prev/next 一致。
 *
 *  @param args  Route.PreviewAudio(fileId, parentCid?)
 *  @param onBack 返回 Files; NavHost 内部 popBackStack
 *  @param nav   Nav 控制器(用于上一首/下一首兄弟跳转)
 */
@Composable
fun PreviewAudioRoute(
    args: Route.PreviewAudio,
    onBack: () -> Unit,
    nav: NavHostController,
) {
    val vm: PreviewAudioViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PreviewAudioViewModel(
                    fileId = args.fileId,
                    parentCid = args.parentCid,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val siblings by vm.siblings.collectAsStateWithLifecycle()
    val playlist by vm.playlist.collectAsStateWithLifecycle()
    val isStarred by vm.isStarred.collectAsStateWithLifecycle()
    PreviewAudioScreen(
        state = state,
        siblings = siblings,
        playlist = playlist,
        isStarred = isStarred,
        onBack = onBack,
        onPrev = { siblings.prevId?.let { nav.navigate(Route.PreviewAudio(it, args.parentCid)) } },
        onNext = { siblings.nextId?.let { nav.navigate(Route.PreviewAudio(it, args.parentCid)) } },
        onToggleStar = vm::toggleStar,
        onSelectFromPlaylist = { fid -> nav.navigate(Route.PreviewAudio(fid, args.parentCid)) },
        onSavePosition = vm::savePosition,
    )
}
