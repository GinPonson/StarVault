package com.starvault.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.starvault.nav.Route

/**
 * PreviewVideo Route 入口（NavHost 唯一注入点）。
 *
 *  与 PreviewImageRoute 几乎对称：把 [Route.PreviewVideo.fileId] 传给 VM。
 *  Compose Navigation 的 type-safe route + savedstate 解出 args，
 *  ViewModel 不需要自己 SavedStateHandle.get()。
 *
 *  @param args  Route.PreviewVideo(fileId)
 *  @param onBack 返回 Files；NavHost 内部 popBackStack
 */
@Composable
fun PreviewVideoRoute(
    args: Route.PreviewVideo,
    onBack: () -> Unit,
) {
    val vm: PreviewVideoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PreviewVideoViewModel(fileId = args.fileId) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    PreviewVideoScreen(state = state, onBack = onBack)
}