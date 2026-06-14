package com.starvault.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.data.model.FileItem

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 *  - 持有 [HomeViewModel]（由 NavBackStackEntry scope 管理）
 *  - 不持有导航副作用：Home 是 BottomNav 的 Tab 之一，所有跳转由 NavHost 解析
 *  - 真实接入 115 后：
 *      - onFileClick → 判断 type：VIDEO/IMAGE/AUDIO 跳 Player，DOC/ZIP 弹预览，OTHER 弹详情
 *      - onFileMore  → 弹 BottomSheetMenu（重命名/分享/删除/打 tag…）
 *      - onTagClick  → VM.setTag(...) 已经在 [setTag] 链路里
 *
 * @param onFileClick    文件行点击：跳详情/播放
 * @param onFileMore     文件行 ⋯ 点击：弹菜单
 * @param vm             注入 ViewModel（Preview 时由测试侧覆写）
 */
@Composable
fun HomeRoute(
    nav: NavHostController,
    onFileClick: (FileItem) -> Unit = {},
    onFileMore: (FileItem) -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onTagClick = vm::setTag,
        onAllTagClick = { vm.setTag(null) },
        onSortClick = { /* Phase 1 stub：排序菜单待 T22 后再接 */ },
        onFabClick = { /* Phase 1 stub：FAB 菜单待 T22 后再接 */ },
        onFileClick = onFileClick,
        onFileMore = onFileMore,
        onQuickClick = { /* Phase 1 stub：4 个快捷入口待 T22 后再接 */ },
    )
}
