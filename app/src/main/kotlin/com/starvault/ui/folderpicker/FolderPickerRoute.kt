package com.starvault.ui.folderpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.core.ToastBus
import com.starvault.nav.Route
import com.starvault.ui.files.FilesScreen
import com.starvault.ui.files.FilesViewModel

/**
 * 文件夹选择器 Route（Files BulkBar MOVE + Preview 屏单文件 MOVE 共用）。
 *
 * **复用 FilesScreen + FilesViewModel**:
 *  - 同一个 FilesViewModel 实例（每 NavBackStackEntry 独立 scope,自动隔离）— 复用
 *    listFolder / setFolder / folderStack 全部已实现的目录树逻辑
 *  - 同一个 FilesScreen Composable — 复用 Crumb / 列表 / 行点击 等视觉
 *  - **不接 NavController**: 选完直接 savedStateHandle["pickedCid"] + popBackStack
 *
 *  关键交互:
 *  1. 点行:
 *     - 是 folder + 不在 excludeIds → 写 savedStateHandle + popBackStack
 *     - 是 folder + 在 excludeIds → ToastBus.error("不能移到自身或子目录") + noop
 *     - 是 file → ToastBus.info("请选择文件夹") + noop
 *  2. 系统 back:
 *     - vm.backFolder() 优先(目录栈内回退)
 *     - 已在根 → onBack() popBackStack 回 Files / Preview
 *
 *  savedStateHandle 回传契约:
 *  - key: "pickedCid" (String) — 选中的目标 folder cid
 *  - 调用方(FilesRoute / PreviewRoute)LaunchedEffect 读 key 触发 moveFiles
 *
 *  为啥不复用 FilesRoute:FilesRoute 接 NavController,会自己 push Preview 等路由,
 *  在 picker 里跳到 Preview 会污染 picker 状态;此处只复用 FilesScreen 视觉层。
 *
 * @param args    Route.FolderPicker(excludeIds)
 * @param nav     NavController(用于 popBackStack + previousBackStackEntry.savedStateHandle)
 * @param onBack  popBackStack 回调(由 StarVaultNavHost 注入)
 */
@Composable
fun FolderPickerRoute(
    args: Route.FolderPicker,
    nav: NavHostController,
    onBack: () -> Unit,
    vm: FilesViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // picker 的 FilesViewModel 自身已有 init { loadFolder("0") },自动从根开始

    FilesScreen(
        state = state,
        onBack = {
            // 优先目录栈回退;已在根 → 退出 picker
            if (!vm.backFolder()) onBack()
        },
        // FolderPicker 不暴露上传/新建文件夹(简化):保持默认 noop
        onAddUpload = { /* picker 不接 upload */ },
        onAddNewFolder = { /* picker 不接 new folder(避免污染目录栈) */ },
        // 单击行 = 选目标文件夹(覆盖 onOpen 默认的 push 子目录行为)
        onOpen = { entry ->
            if (!entry.isFolder) {
                // 文件行不允许选(picker 目标必须是 folder)
                ToastBus.info("请选择文件夹")
                return@FilesScreen
            }
            if (entry.id in args.excludeIds) {
                // 移到自身或子目录会自循环,115 端拒
                ToastBus.error("不能移到自身或子目录")
                return@FilesScreen
            }
            // 回传 pickedCid 给上一个 back stack entry(Files / Preview)
            nav.previousBackStackEntry
                ?.savedStateHandle
                ?.set("pickedCid", entry.id)
            nav.popBackStack()
        },
        // picker 上下文里:点击 folder 行(默认是 onOpen 切子目录)— 上面已覆盖为"选中目标"
        // 这里 onSelect 走默认(行选择)对 picker 无意义,noop
        onSelect = { /* picker 不暴露多选 */ },
        // picker 上下文里 sort / view toggle / search / transfers / more 不需要,但保留 onSort 让
        // 用户能找到目标;其余 onSearch / onTransfers / onMore / onViewToggle 在 FilesScreen
        // 默认 onClick 是 noop,不传也不会崩
        onSort = { vm.applySort(
            // 切换排序时复用当前 (order, asc) 模式;简化:picker 内不暴露排序,noop
            // — 但为了 picker 内文件夹查找体验,先保留 onSort
            // (此处省略具体参数,让 caller 传 vm.applySort — 实际 caller 必须显式给参数)
            order = "user_ptime", asc = 0,
        ) },
        onSearch = { /* picker 不暴露搜索 */ },
        onTransfers = { /* picker 不暴露传输中心 */ },
        onMore = { /* picker 不暴露更多 */ },
        onViewToggle = { /* picker 不切换视图模式 */ },
        onCrumbClick = { index -> vm.popToFolder(index) },
        onCloseBulk = { /* picker 无 BulkBar */ },
        onBulkAction = { /* picker 无 BulkBar */ },
        onDownload = { /* picker 不暴露下载 */ },
        onLoadMore = vm::loadMore,
    )
}
