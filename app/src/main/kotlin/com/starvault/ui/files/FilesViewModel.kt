package com.starvault.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.toFileType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Files 屏 ViewModel — 接 115 webapi /files 真实数据。
 *
 *  - init { loadFolder("0") } : 启动即拉根目录
 *  - setFolder(cid) : 跳进子文件夹；切换时取消 in-flight loadJob + 清选中
 *  - refresh() : 当前目录重新拉（pull-to-refresh 触发）
 *  - selectType / changeViewMode / toggleSelect / clearSelection / bulk 仍走本地态（不上行）
 *
 *  状态机：
 *    Loading → Success（list 非空）
 *            → Success（list 空但请求成功，显示空状态）
 *            → Error（请求失败，message 来自异常）
 *
 *  错误策略：
 *    网络/解析失败 → FilesUiState.Error(message)；UI 屏已有 Error 分支处理
 *    partial-success（仅文件夹或仅文件成功）由 [FilesRepository] 内部消化，对 UI 是正常 Success
 */
class FilesViewModel(
    private val filesRepository: FilesRepository = ServiceLocator.filesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FilesUiState>(FilesUiState.Loading())
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    /** 当前目录 cid；用于 setFolder / refresh 复用。 */
    private var currentCid: String = "0"

    /**
     * 当前排序偏好（115 webapi `o` + `asc` 参数）。
     *  - 切目录时保留（用户偏好）
     *  - 默认值与 115 webapi 默认一致：修改时间降序
     *  - 由 [applySort] 写入
     */
    private var currentOrder: String = FilesRepository.DEFAULT_ORDER
    private var currentAsc: Int = FilesRepository.DEFAULT_ASC

    /** 当前目录的路径快照（根→当前）。Success 状态携带，Crumb 直接渲染。 */
    private var currentPath: List<FolderCrumb> = listOf(FolderCrumb("0", "我的文件"))

    /**
     * 目录栈：根为 [("0", "我的文件")]，进入子目录 push (cid, name)。
     *  - setFolder(cid, name) ：push (cid, name) → loadFolder(cid)
     *  - backFolder()         ：pop 栈顶 → loadFolder(新栈顶)；栈长=1 时返回 false（已到根）
     *  - popToFolder(index)   ：截断到 index+1 → loadFolder(该 cid)；用于点击 Crumb 中段
     *
     *  栈机制让系统 back / 「返回上一级」按钮 / Crumb 点击都能逐层回退到根。
     *  name 由调用方传入（来自 FileEntry.name），保证路径显示 = 用户实际点的名字。
     */
    private val folderStack: ArrayDeque<FolderCrumb> =
        ArrayDeque<FolderCrumb>().apply { addLast(FolderCrumb("0", "我的文件")) }

    /** 当前正在跑的拉取任务；切换目录或 refresh 时取消旧任务。 */
    private var loadJob: Job? = null

    /**
     * 独立的"加载更多"任务。**不与 loadJob 互斥**——分页是并发的：
     * 切目录时取消 loadJob，但已有的 loadMore 也会被下面 markPending() 期间的状态变更
     * 自然覆盖（hasMore 重置、isLoadingMore 旧值不再被读）。
     * 用独立 Job 是为了 loadMore 之间不互相 cancel（连滚到底时可能两个 loadMore 排队）。
     */
    private var loadMoreJob: Job? = null

    /**
     * 分页状态：
     *  - currentOffset : 下一页要用的 offset（首屏后 = 已加载 size）
     *  - currentCid    : 当前目录；loadMore / setFolder / backFolder 都用它
     * 切目录 / tab 切换时 reset 到 0。
     */
    private var currentOffset: Int = 0

    init {
        // 首屏不再走 Loading 分支（避免全屏"加载中…"闪现）：
        // 先放一个空 Success + pendingLoad=true，让 UI 立即渲染 AppBar/Crumb/Toolbar
        // + 列表 skeleton（all=empty），顶部进度条在数据到达前持续显示。
        // 数据到达后由 loadFolder() 的 onSuccess 直接覆盖此 state。
        _state.value = FilesUiState.Success(
            folderId = currentCid,
            folderPath = currentPath,
            all = emptyList(),
            activeType = null,
            viewMode = ViewMode.LIST,
            selectedIds = emptySet(),
            sortLabel = formatSortLabel(currentOrder, currentAsc),
            totalCount = 0,
            hasMore = false,
            isLoadingMore = false,
            pendingLoad = true,
        )
        loadJob = viewModelScope.launch { loadFolder(cid = "0", offset = 0) }
    }

    /**
     * 切换目录（点击文件夹行触发）。
     *
     * @param folderId  目标 cid；null 视为根 "0"
     * @param folderName 该文件夹显示名；进入子目录时 Crumb 用它，
     *                   null 时用 cid 占位（深链 / 测试场景）
     */
    fun setFolder(folderId: String?, folderName: String? = null) {
        val cid = folderId ?: "0"
        if (cid == currentCid && _state.value is FilesUiState.Success) return
        folderStack.addLast(FolderCrumb(cid, folderName ?: cid))
        currentCid = cid
        currentPath = folderStack.toList()
        currentOffset = 0
        // 切目录前：取消 loadMore（避免旧目录的下一页覆盖新列表）
        loadMoreJob?.cancel()
        markPending()
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(cid, offset = 0) }
    }

    /**
     * 返回上一级目录。栈长=1（已在根）时返回 false，让 Route 端继续 popBackStack。
     * 栈长>1 时 pop 出栈顶后重新 load 新的栈顶目录。
     */
    fun backFolder(): Boolean {
        if (folderStack.size <= 1) return false
        folderStack.removeLast()
        val top = folderStack.last()
        currentCid = top.cid
        currentPath = folderStack.toList()
        currentOffset = 0
        loadMoreJob?.cancel()
        markPending()
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(top.cid, offset = 0) }
        return true
    }

    /**
     * 跳回到 Crumb 路径中的某一段（index，0-based）。截断 stack 到 index+1 后重新加载。
     * - 0 → 根 "我的文件"
     * - 1 → 第一级子目录
     * 传 null 视作根（=index=0）。
     */
    fun popToFolder(index: Int) {
        if (index < 0 || index >= folderStack.size) return
        // 截断到 index+1，保留 0..index 的所有段
        while (folderStack.size > index + 1) folderStack.removeLast()
        val top = folderStack.last()
        currentCid = top.cid
        currentPath = folderStack.toList()
        currentOffset = 0
        loadMoreJob?.cancel()
        markPending()
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(top.cid, offset = 0) }
    }

    /** 重新拉当前目录（pull-to-refresh）。重置分页回首屏。 */
    fun refresh() {
        currentOffset = 0
        loadMoreJob?.cancel()
        markPending()
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadFolder(currentCid, offset = 0) }
    }

    /**
     * 应用新的排序偏好。
     *
     * - 写入 [_currentOrder] / [_currentAsc]（跨目录保留）
     * - sortLabel 立刻更新（无网络往返）
     * - 重新拉当前目录（offset 归零），按新排序加载首屏
     * - 已有列表保留 + pendingLoad=true，避免全屏 Loading 闪屏
     *
     * @param order 115 webapi `o` 参数（user_ptime / user_utime / user_intime / file_size / file_name / file_type）
     * @param asc   升降序：0 = 降序，1 = 升序
     */
    fun applySort(order: String, asc: Int) {
        currentOrder = order
        currentAsc = asc
        val s = _state.value as? FilesUiState.Success ?: run {
            // Loading/Error 态下更新 sortLabel 等下一屏渲染
            return
        }
        _state.value = s.copy(sortLabel = formatSortLabel(order, asc))
        refresh()
    }

    /**
     * 加载下一页。**UI 滚到列表底部时调用**。
     *
     *  并发防护：
     *  - isLoadingMore=true 时直接 return（Fast Scroll 不会重复触发）
     *  - hasMore=false 时直接 return（已到底）
     *  - currentCid 与当前 Success.folderId 不一致时 return（用户已切走，旧请求结果丢弃）
     *
     *  数据合并：append 到 s.all 末尾，distinctBy(id) 兜底防 115 跨页偶发重复。
     *  totalCount 更新策略：已加载 size 与 115 count 取较大值——
     *  - 拉下一页过程中用户可能新删除文件，count 会变小；用 max 防 "已加载 60 / 共 50" 倒挂
     *  - 也避免初次 totalCount = 50 但 all.size 一直在涨，UI 顶部数显需要及时同步
     */
    fun loadMore() {
        val s = _state.value as? FilesUiState.Success ?: return
        if (s.isLoadingMore || !s.hasMore) return
        if (s.folderId != currentCid) return
        val offset = currentOffset
        _state.value = s.copy(isLoadingMore = true)
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            filesRepository.listFolder(
                cid = currentCid,
                offset = offset,
                order = currentOrder,
                asc = currentAsc,
            )
                .onSuccess { page ->
                    // 二次防护：拉回来时目录已切走 → 丢弃结果
                    val current = _state.value as? FilesUiState.Success ?: return@onSuccess
                    if (current.folderId != currentCid) return@onSuccess
                    val merged = (current.all + page.items.map { it.toFileEntry() })
                        .distinctBy { it.id }
                    currentOffset = offset + page.items.size
                    _state.value = current.copy(
                        all = merged,
                        // ⚠️ totalCount 用已加载数（merged.size），不用 115 的 `count`：
                        // 115 在某些目录会返回远超本目录实际子项的 `count`（疑似历史遗留或
                        // 全账号汇总），直接展示会让"共 N 项"显示到上万。已加载数 + 底部
                        // loading indicator 是更可靠的"还有更多"信号。
                        totalCount = merged.size,
                        totalServerCount = page.totalCount,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                    )
                }
                .onFailure {
                    val current = _state.value as? FilesUiState.Success ?: return@onFailure
                    if (current.folderId != currentCid) return@onFailure
                    // 失败不弹 Error 屏（已有列表可用），仅清 isLoadingMore；UI 列表底部可考虑 toast
                    _state.value = current.copy(isLoadingMore = false)
                }
        }
    }

    /**
     * 保留当前 Success 列表渲染，加 pendingLoad=true 让 UI 显示顶部细进度条。
     * init 首次加载无 Success 时不动作，仍由 loadFolder 切到 Loading。
     */
    private fun markPending() {
        val s = _state.value as? FilesUiState.Success ?: return
        if (s.pendingLoad) return
        _state.value = s.copy(pendingLoad = true)
    }

    private suspend fun loadFolder(cid: String, offset: Int) {
        // 不再立即覆盖为 Loading：markPending 已保留旧列表 + pendingLoad
        val previousSelected = (_state.value as? FilesUiState.Success)?.selectedIds ?: emptySet()
        val previousViewMode = (_state.value as? FilesUiState.Success)?.viewMode ?: ViewMode.LIST
        val previousSortLabel = formatSortLabel(currentOrder, currentAsc)
        val previousActiveType = (_state.value as? FilesUiState.Success)?.activeType

        filesRepository.listFolder(
            cid = cid,
            offset = offset,
            order = currentOrder,
            asc = currentAsc,
        )
            .onSuccess { page ->
                val entries = page.items.map { it.toFileEntry() }
                currentOffset = offset + page.items.size
                _state.value = FilesUiState.Success(
                    folderId = cid,
                    folderPath = currentPath,
                    all = entries,
                    activeType = previousActiveType,
                    viewMode = previousViewMode,
                    selectedIds = previousSelected.filterSelected(entries).toSet(),
                    sortLabel = previousSortLabel,
                    // ⚠️ 顶部 "共 N 项" 用已加载数（entries.size），不用 115 的 `count`——
                    // 115 在某些目录会返回远超本目录实际子项的 `count`（疑似全账号汇总），
                    // 直接展示会让"共 N 项"显示到上万。已加载数 + 底部 loading 是更可靠的
                    // "还有更多"信号；totalServerCount 单独留字段供后续做"已加载 X / 共 Y" 增量 UI。
                    totalCount = entries.size,
                    totalServerCount = page.totalCount,
                    hasMore = page.hasMore,
                    isLoadingMore = false,
                    pendingLoad = false,
                )
            }
            .onFailure { e ->
                // 失败：清空旧列表，进 Error 态；如有旧列表保留会更复杂，先简化
                _state.value = FilesUiState.Error(
                    message = e.message ?: "文件列表加载失败",
                )
            }
    }

    /* ─────────────────── 本地态（不上行） ─────────────────── */

    fun selectType(type: FileType?) {
        val s = _state.value as? FilesUiState.Success ?: return
        // ⚠️ tab 切换会改变 115 端筛选后的 totalCount（fc/fe 参数），所以应重新拉
        // 当前实现：仅做本地 filter（"全部" 50 + "图片" 48 都从同一份拉到的数据里筛）。
        // 这是个已知简化——服务端筛选待切到分页 endpoint 增量时再做。
        // tabCounts 仍按已加载数据算；totalCount 同理（避免顶部跳数）。hasMore 不重置，
        // 滚到末尾时 loadMore 仍会拉到下一原始页（含其它类型），UI 再过滤显示。
        _state.value = s.copy(
            activeType = type,
            totalCount = s.all.count { e -> type == null || e.type == type },
        )
    }

    fun changeViewMode(mode: ViewMode) {
        val s = _state.value as? FilesUiState.Success ?: return
        _state.value = s.copy(viewMode = mode)
    }

    fun toggleSelect(id: String) {
        val s = _state.value as? FilesUiState.Success ?: return
        val next = s.selectedIds.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
        _state.value = s.copy(selectedIds = next)
    }

    fun clearSelection() {
        val s = _state.value as? FilesUiState.Success ?: return
        _state.value = s.copy(selectedIds = emptySet())
    }

    fun bulk(action: BulkAction) {
        // Phase 2 stub：仅清掉选中；后续切片接入真 bulk endpoint
        clearSelection()
    }

    /* ─────────────────── ParsedFileItem → FileEntry 映射 ─────────────────── */

    /**
     * 把 115 DTO 映射成 UI 用的 [FileEntry]，附带 metaSegments（屏幕渲染时拼 " · "）。
     *
     *  策略：
     *  - 文件夹: [子项数（暂无，先用 "—"） , 相对时间]
     *    — FilesRepository 不返回子项数（115 /files count 是当前筛选下的条数而非子项数），
     *    暂显示 "—"；后续切片可调 /files?cid=...&fc=count 单独拿
     *  - 文件: [格式化大小, 时长（audio/video） / "—", 相对时间]
     */
    private fun ParsedFileItem.toFileEntry(): FileEntry {
        val type = toFileType()
        val sizeText = if (isFolder) "—" else formatBytes(sizeBytes)
        val durationText = when {
            isFolder -> ""
            type == FileType.VIDEO || type == FileType.AUDIO -> formatDuration(playLong)
            else -> ""
        }
        val timeText = formatRelativeTime(mtimeSec)
        val meta = mutableListOf<String>()
        if (sizeText.isNotEmpty() && sizeText != "—") meta += sizeText
        if (durationText.isNotEmpty()) meta += durationText
        if (timeText.isNotEmpty()) meta += timeText
        // thumbnailUrl: 仅 IMAGE / VIDEO 携带；其它类型留 null，FileRow 用渐变色块 fallback
        val thumb = when {
            isFolder -> null
            type == FileType.IMAGE || type == FileType.VIDEO -> thumbnailUrl.takeIf { it.isNotEmpty() }
            else -> null
        }
        return FileEntry(
            id = id,
            name = name,
            type = type,
            metaSegments = meta,
            isFolder = isFolder,
            thumbnailUrl = thumb,
        )
    }
}

/** 字节数 → "1.04 TB" / "32.4 MB" 风格（1 位小数）。与 ProfileViewModel 同款实现。 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024
    return when {
        bytes >= tb -> "%.1f TB".format(bytes.toDouble() / tb)
        bytes >= gb -> "%.1f GB".format(bytes.toDouble() / gb)
        bytes >= mb -> "%.1f MB".format(bytes.toDouble() / mb)
        bytes >= kb -> "%.1f KB".format(bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

/** 秒数 → "01:30:08" 或 "04:23"。 */
private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = TimeUnit.SECONDS.toHours(seconds.toLong())
    val m = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Unix 秒 → 中文相对时间（"2 小时前" / "昨天 23:14" / "今天 14:22" / "3 天前" / "2026/06/14"）。 */
private fun formatRelativeTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSec
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60} 分钟前"
        diff < 24 * 3600 -> "${diff / 3600} 小时前"
        diff < 7 * 24 * 3600 -> "${diff / (24 * 3600)} 天前"
        else -> {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochSec * 1000 }
            "%04d/%02d/%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
            )
        }
    }
}

/** 切目录后过滤掉不属于新列表的 id（防止选中态泄漏）。 */
private fun Set<String>.filterSelected(currentEntries: List<FileEntry>): List<String> {
    val valid = currentEntries.map { it.id }.toSet()
    return this.filter { it in valid }
}

enum class BulkAction { DOWNLOAD, SHARE, MOVE, RENAME, DELETE }

/* ─────────────────── 排序字段映射 ─────────────────── */

/**
 * 115 webapi `o` 参数 → UI 显示文案的映射。
 *
 *  - field : 115 协议字段名（传给 listFolder / searchFiles）
 *  - label  : SectionHead 显示文案（"按 X ▾"）
 *  - asc    : 升降序（0 = 降序，1 = 升序）
 *
 *  排序 6 字段 × 2 方向 = 12 个组合，BottomSheet 一级菜单用 [SORT_FIELDS] 列字段，
 *  点中后二级菜单用 [formatSortLabel] 拼"按 X ▾/▴"决定当前态。
 *
 *  **字段语义**（按 p115client docstring 10440-10447 校正）：
 *    - `user_ptime`  = 创建时间
 *    - `user_utime`  = 修改时间
 *    - `file_name`   = 文件名
 *    - `file_size`   = 文件大小
 *    - `file_type`   = 文件种类
 *    - `user_otime`  = 上次打开时间（p115client 支持，但显示语义弱，不放 MVP）
 */
internal data class SortOption(
    val field: String,
    val label: String,
)

internal val SORT_FIELDS: List<SortOption> = listOf(
    SortOption("user_ptime",  "创建时间"),
    SortOption("user_utime",  "修改时间"),
    SortOption("user_otime",  "上次打开时间"),
    SortOption("file_size",   "文件大小"),
    SortOption("file_name",   "文件名"),
    SortOption("file_type",   "文件类型"),
)

/**
 * 把 (field, asc) 拼成 SectionHead 显示文案：
 * - "按修改时间 ▾"  (asc=0)
 * - "按修改时间 ▴"  (asc=1)
 *
 * 未知字段降级到 "按修改时间 ▾"。
 */
internal fun formatSortLabel(field: String, asc: Int): String {
    val label = SORT_FIELDS.firstOrNull { it.field == field }?.label ?: "创建时间"
    val arrow = if (asc == 1) " ▴" else " ▾"
    return "按$label$arrow"
}