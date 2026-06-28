package com.starvault.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.data.model.FileType
import com.starvault.nav.Route
import com.starvault.ui.files.FileEntry

/**
 * 搜索屏 Route 入口（NavHost 唯一注入点）。
 *
 *  跳转逻辑（来自 FilesRoute.onSearch → nav.navigate(Route.Search())）：
 *  - 点 result IMAGE/VIDEO → PreviewImage / PreviewVideo（点文件夹 MVP noop）
 *  - 系统返回 → nav.popBackStack() 回到 Files 屏
 *
 *  @param args  Route.Search(initialQuery: String) — 由 FilesRoute onSearch 触发，默认空 query
 *  @param nav   Nav 控制器
 *  @param vm    注入 ViewModel（ServiceLocator.filesRepository 默认）
 */
@Composable
fun SearchRoute(
    args: Route.Search,
    nav: NavHostController,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // 第一次进入：如果 initialQuery 非空（如从 deep link 进来），回填
    androidx.compose.runtime.LaunchedEffect(args.initialQuery) {
        if (args.initialQuery.isNotEmpty() && state.query != args.initialQuery) {
            vm.onQueryChange(args.initialQuery)
        }
    }

    SearchScreen(
        state = state,
        onQueryChange = vm::onQueryChange,
        onClearQuery = vm::clearQuery,
        onLoadMore = vm::loadMore,
        onOpen = { e ->
            // 文件夹：MVP noop（Future: 跳回 Files 屏对应 cid）
            // IMAGE/VIDEO：跳 Preview
            // 其它类型：MVP noop
            when {
                e.isFolder -> { /* TODO: deep link 到 Files 屏对应 cid */ }
                e.type == FileType.IMAGE -> nav.navigate(Route.PreviewImage(e.id))
                e.type == FileType.VIDEO -> nav.navigate(Route.PreviewVideo(e.id))
                e.type == FileType.AUDIO -> nav.navigate(Route.PreviewAudio(e.id))
                else -> { /* TODO: 详情 / 下载 */ }
            }
        },
        onBack = { nav.popBackStack() },
    )
}