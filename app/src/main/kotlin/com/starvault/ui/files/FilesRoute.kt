package com.starvault.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.data.model.FileType
import com.starvault.nav.Route
import com.starvault.ui.dialog.ConfirmDialog
import com.starvault.ui.dialog.RenameDialog
import com.starvault.ui.files.sort.SortSheet
import com.starvault.ui.transfers.TransfersViewModel
import com.starvault.ui.upload.rememberUploadLauncher

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
    transfersViewModel: TransfersViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 上传入口:ActivityResultContracts.GetContent → queryFileMeta → UploadWorker.enqueue + observeWork
    // currentCid 在 picker 回调里实时读 state(folderId 可能在用户操作期间变了 — 比如进子目录后再上传)
    val launchPicker = rememberUploadLauncher { meta ->
        val liveCid = (vm.state.value as? com.starvault.ui.files.FilesUiState.Success)?.folderId ?: "0"
        com.starvault.ui.upload.enqueueUpload(
            context = context,
            uri = meta.uri,
            targetCid = liveCid,
            transfersViewModel = transfersViewModel,
        )
    }

    // 启动时按 args 切目录（null = 根 "0"，VM.init 已默认加载）
    LaunchedEffect(args.folderId) {
        if (args.folderId != null) vm.setFolder(args.folderId)
    }

    // 排序 BottomSheet 显隐（rememberSaveable：旋转屏保留）
    var sortSheetVisible by rememberSaveable { mutableStateOf(false) }

    // 新建文件夹 AlertDialog 显隐（rememberSaveable：旋转屏保留）
    var newFolderDialogVisible by rememberSaveable { mutableStateOf(false) }

    // M5 CRUD 弹层 state:
    //  - confirmDialog:删除确认(选 N 项 → 弹 N 项确认)
    //  - renameDialog:重命名(仅 N==1 时由 bulk 弹);用 FileEntry? 存要重命名的行
    //  - moveExcludeIds:FolderPicker 排除规则(当前目录 cid + 已选 id 集合)
    //
    //  注意:这些 state 不存到 savedStateHandle(弹层是瞬时 UI,旋转屏关掉无所谓;用户能理解)
    var pendingDeleteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingRenameEntry by remember { mutableStateOf<FileEntry?>(null) }

    // 监听 FolderPicker 选完返回的 pickedCid(savedStateHandle 回传,见 FolderPickerRoute)
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("pickedCid", null)?.collect { pickedCid ->
            if (pickedCid != null) {
                val success = vm.state.value as? com.starvault.ui.files.FilesUiState.Success
                if (success != null && success.selectedIds.isNotEmpty()) {
                    vm.moveFiles(ids = success.selectedIds.toList(), toCid = pickedCid)
                }
                // 一次性 key,消费后清掉,避免重组重复触发
                savedStateHandle["pickedCid"] = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FilesScreen(
            state = state,
            onBack = {
                // 先尝试目录层级回退；栈已到根时再 popBackStack 退出 Files
                if (!vm.backFolder()) nav.popBackStack()
            },
            onSearch = { nav.navigate(Route.Search()) },
            onTransfers = { nav.navigate(Route.Transfers) },
            onMore = { /* TODO: 弹文件更多菜单 */ },
            onTypeClick = vm::selectType,
            onViewToggle = vm::changeViewMode,
            onSort = { sortSheetVisible = true },
            onSelect = { e -> vm.toggleSelect(e.id) },
            onOpen = { e ->
                // 文件夹 → 切子目录
                // IMAGE   → PreviewImage
                // VIDEO   → PreviewVideo(带上 parentCid 供上一集/下一集兄弟导航)
                // AUDIO / DOC / ZIP / OTHER → 当前 phase 不接预览,noop(后续 phase)
                when {
                    e.isFolder -> vm.setFolder(e.id, e.name)
                    e.type == FileType.IMAGE -> nav.navigate(Route.PreviewImage(e.id))
                    e.type == FileType.VIDEO -> nav.navigate(Route.PreviewVideo(e.id, e.parentId))
                    e.type == FileType.AUDIO -> nav.navigate(Route.PreviewAudio(e.id, e.parentId))
                    else -> { /* TODO: 详情页 / 下载 */ }
                }
            },
            // M3: row 右侧 "···" DropdownMenu 触发单文件下载
            onDownload = { e -> vm.downloadEntry(e) },
            onCrumbClick = { index -> vm.popToFolder(index) },
            onCloseBulk = vm::clearSelection,
            // M5:bulk 5 路 when 在 VM 内;UI 层根据 result 弹弹层
            onBulkAction = { action ->
                when (action) {
                    com.starvault.ui.files.BulkAction.DELETE -> {
                        val success = vm.state.value as? com.starvault.ui.files.FilesUiState.Success
                        val ids = success?.selectedIds?.toList().orEmpty()
                        if (ids.isNotEmpty()) pendingDeleteIds = ids
                    }
                    com.starvault.ui.files.BulkAction.RENAME -> {
                        val success = vm.state.value as? com.starvault.ui.files.FilesUiState.Success
                        val ids = success?.selectedIds?.toList().orEmpty()
                        // N==1 才弹 dialog(VM 已 ToastBus 拒绝 N>1;UI 双保险)
                        if (ids.size == 1) {
                            pendingRenameEntry = success?.all?.find { it.id == ids.first() }
                        }
                    }
                    com.starvault.ui.files.BulkAction.MOVE -> {
                        val success = vm.state.value as? com.starvault.ui.files.FilesUiState.Success
                        val ids = success?.selectedIds.orEmpty()
                        if (ids.isNotEmpty()) {
                            // 跳 FolderPicker;excludeIds = 当前 cid + 已选 id 集合
                            // (防止移到自身或已选文件夹)
                            val currentCid = success?.folderId ?: "0"
                            val exclude = (ids + currentCid).toList()
                            nav.navigate(Route.FolderPicker(excludeIds = exclude))
                        }
                    }
                    // DOWNLOAD / SHARE 由 VM 内部直接处理,UI 不弹层
                    com.starvault.ui.files.BulkAction.DOWNLOAD,
                    com.starvault.ui.files.BulkAction.SHARE -> vm.bulk(action)
                }
            },
            onAddUpload = { launchPicker() },
            onAddNewFolder = { newFolderDialogVisible = true },
            onLoadMore = vm::loadMore,
        )

        // 排序 BottomSheet（覆盖在 FilesScreen 之上）
        if (sortSheetVisible && state is com.starvault.ui.files.FilesUiState.Success) {
            val success = state as com.starvault.ui.files.FilesUiState.Success
            SortSheet(
                currentField = success.sortField,
                currentAsc = success.sortAsc,
                onPicked = { field, asc ->
                    vm.applySort(field, asc)
                    sortSheetVisible = false
                },
                onDismiss = { sortSheetVisible = false },
            )
        }

        // 新建文件夹 AlertDialog（覆盖在 FilesScreen 之上）
        if (newFolderDialogVisible) {
            NewFolderDialog(
                onConfirm = { name ->
                    vm.createFolder(name)
                    newFolderDialogVisible = false
                },
                onDismiss = { newFolderDialogVisible = false },
            )
        }

        // M5:删除确认弹层
        if (pendingDeleteIds.isNotEmpty()) {
            ConfirmDialog(
                title = "确认删除 ${pendingDeleteIds.size} 项?",
                message = "此操作会移到回收站，7 天内可在 115 端恢复",
                confirmLabel = "删除",
                danger = true,
                onConfirm = {
                    vm.deleteFiles(pendingDeleteIds)
                    pendingDeleteIds = emptyList()
                },
                onDismiss = { pendingDeleteIds = emptyList() },
            )
        }

        // M5:重命名弹层(仅 N==1)
        pendingRenameEntry?.let { entry ->
            RenameDialog(
                currentName = entry.name,
                onConfirm = { newName ->
                    vm.renameFile(entry.id, newName)
                    pendingRenameEntry = null
                },
                onDismiss = { pendingRenameEntry = null },
            )
        }
    }
}
