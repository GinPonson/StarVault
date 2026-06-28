package com.starvault.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.starvault.nav.Route

/**
 * PreviewImage Route 入口（NavHost 唯一注入点）。
 *
 *  通过自定义 ViewModelFactory 把 [Route.PreviewImage.fileId] 传给 VM 构造参数，
 *  这是 Compose Navigation type-safe route + savedstate 的标准用法：
 *  ViewModel 不再单独 SavedStateHandle 解析（route 已在 args 里 type-safe 拆好）。
 *
 *  @param args  Route.PreviewImage(fileId) — 来自 NavHost composable<Route.PreviewImage>
 *  @param onBack 返回 Files；NavHost 内部 popBackStack
 */
@Composable
fun PreviewImageRoute(
    args: Route.PreviewImage,
    onBack: () -> Unit,
) {
    val vm: PreviewImageViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PreviewImageViewModel(fileId = args.fileId) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val isStarred by vm.isStarred.collectAsStateWithLifecycle()
    PreviewImageScreen(state = state, isStarred = isStarred, onBack = onBack, onToggleStar = vm::toggleStar)
}