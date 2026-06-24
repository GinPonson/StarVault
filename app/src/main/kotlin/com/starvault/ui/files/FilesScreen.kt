package com.starvault.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import com.starvault.component.Icons
import com.starvault.data.model.FileType
import com.starvault.theme.StarVaultTheme

/**
 * Files 屏（对应 design/06-files.html 的"我的文件" 1:1 复刻）。
 *
 * 整体结构（垂直滚动 / 网格）：
 *   ┌─ AppBar       "我的文件" + 4 icon-btn（搜索 / 传输 / 更多 / 新建）
 *   ├─ Crumb        多段 "我的文件 / 手机相册 / 涩图" + 末尾 "28 项"
 *   ├─ Toolbar      6 tab（全部/视频/图片/文档/音频/其他）+ 列表/网格 toggle
 *   ├─ SectionHead  "共 28 项" + "按修改时间 ▾"
 *   ├─ List         10 file-row（列表或 2 列网格）
 *   └─ BulkBar      选中时浮出（已选 N 项 + 5 acts + close）
 *   +  FAB          右下角 "+" 上传
 *
 * 状态映射（与 [FilesUiState] 对应）：
 *   - Loading : 占位
 *   - Success : 完整渲染
 *   - Error   : message
 */
@Composable
fun FilesScreen(
    state: FilesUiState,
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onTransfers: () -> Unit = {},
    onMore: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    onTypeClick: (FileType?) -> Unit = {},
    onViewToggle: (ViewMode) -> Unit = {},
    onSort: () -> Unit = {},
    onSelect: (FileEntry) -> Unit = {},
    onOpen: (FileEntry) -> Unit = {},
    /** 点击面包屑中某一段（0-based index）；截断 stack 跳回该层。 */
    onCrumbClick: (Int) -> Unit = {},
    onCloseBulk: () -> Unit = {},
    onBulkAction: (BulkAction) -> Unit = {},
    onUpload: () -> Unit = {},
    /**
     * 列表滚到末尾时触发；ViewModel 用它加载下一页（offset+limit）。
     * UI 侧用 `LazyListState.firstVisibleItemIndex` + visibleItemCount 判别接近末尾时调一次。
     */
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    // 拦截系统 back：Files 在 bottom-nav tab，无父栈；让 onBack 决定回上一级目录或退出屏
    BackHandler(enabled = true) { onBack() }
    Box(modifier = modifier.fillMaxSize().background(c.bg)) {
        when (state) {
            is FilesUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("加载中…", style = t.body, color = c.muted)
                }
            }
            is FilesUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.message, style = t.body, color = c.danger)
                }
            }
            is FilesUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    FilesAppBar(
                        onBack = onBack,
                        onSearch = onSearch,
                        onTransfers = onTransfers,
                        onMore = onMore,
                        onNewFolder = onNewFolder,
                    )
                    Crumb(
                        folderPath = state.folderPath,
                        totalCount = state.all.size,
                        onCrumbClick = onCrumbClick,
                    )
                    // 切目录/refresh 时顶部细进度条，旧列表保留渲染避免全屏闪烁
                    if (state.pendingLoad) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = c.accent,
                            trackColor = c.border,
                        )
                    }
                    Toolbar(
                        activeType = state.activeType,
                        counts = state.tabCounts,
                        viewMode = state.viewMode,
                        onTypeClick = onTypeClick,
                        onViewToggle = onViewToggle,
                    )
                    SectionHead(
                        visibleCount = state.totalCount,
                        sortLabel = state.sortLabel,
                        onSort = onSort,
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state.viewMode) {
                            ViewMode.LIST -> FileList(
                                files = visible(state),
                                selectedIds = state.selectedIds,
                                onSelect = onSelect,
                                onOpen = onOpen,
                                isLoadingMore = state.isLoadingMore,
                                hasMore = state.hasMore,
                                onLoadMore = onLoadMore,
                            )
                            ViewMode.GRID -> FileGrid(
                                files = visible(state),
                                selectedIds = state.selectedIds,
                                onSelect = onSelect,
                                onOpen = onOpen,
                                isLoadingMore = state.isLoadingMore,
                                hasMore = state.hasMore,
                                onLoadMore = onLoadMore,
                            )
                        }
                    }
                }
                // Bulk bar（仅在选中时）
                if (state.selectedIds.isNotEmpty()) {
                    BulkBar(
                        count = state.selectedIds.size,
                        onClose = onCloseBulk,
                        onAction = onBulkAction,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
                // FAB
                Fab(
                    onClick = onUpload,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = if (state.selectedIds.isNotEmpty()) 80.dp else 90.dp),
                )
            }
        }
    }
}

private fun visible(s: FilesUiState.Success): List<FileEntry> =
    if (s.activeType == null) s.all else s.all.filter { it.type == s.activeType }

/* ───────────────────── AppBar ───────────────────── */

@Composable
private fun FilesAppBar(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onTransfers: () -> Unit,
    onMore: () -> Unit,
    onNewFolder: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "我的文件",
            style = t.large,
            color = c.fg,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn(icon = Icons.Search,     onClick = onSearch,    contentDescription = "搜索")
            IconBtn(icon = Icons.Transfers,  onClick = onTransfers, contentDescription = "传输中心")
            IconBtn(icon = Icons.More,       onClick = onMore,      contentDescription = "更多")
            IconBtn(icon = Icons.NewFolder,  onClick = onNewFolder, contentDescription = "新建文件夹")
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = StarVaultTheme.colors.fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ───────────────────── Crumb ───────────────────── */

/**
 * 路径面包屑（对应 design HTML `.crumb`）。
 *
 *  - 根目录：[("0", "我的文件")] → "我的文件  /  50 项"
 *  - 进一层：[("0", "我的文件"), ("cidA", "手机相册")] → "我的文件  /  手机相册  /  50 项"
 *  - 进三层：再加一段 "涩图"
 *
 *  - 每段都可点 → [onCrumbClick](index) 截断 stack 跳回该层
 *  - 最后一段（当前目录）加粗 + 无下划线，表示当前位置
 *  - 其余段 muted 色 + 点击高亮色（复用 c.fg），强化可点感
 *  - 段数过多时保持单行（softWrap=false），右侧 "N 项" 推到末尾并 ellipsis 整段路径
 *
 *  design HTML `.crumb .root` 是 fg+SemiBold；中间段是 muted；我们这里把 root 加粗、
 *  其余段 muted，与原设计一致。
 */
@Composable
private fun Crumb(
    folderPath: List<FolderCrumb>,
    totalCount: Int,
    onCrumbClick: (Int) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 路径段：folderPath = [根, A, B, ...]
        folderPath.forEachIndexed { index, crumb ->
            val isLast = index == folderPath.lastIndex
            Text(
                text = crumb.name,
                style = t.caption,
                color = if (isLast) c.fg else c.muted,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                // 末段不可点（已是当前）；其余段可点跳回
                modifier = if (!isLast) {
                    Modifier.clickable { onCrumbClick(index) }
                } else {
                    Modifier
                },
                maxLines = 1,
                softWrap = false,
            )
            if (!isLast) {
                Text(
                    text = "/",
                    style = t.caption,
                    color = Color(0xFFC4C4C4),
                )
            }
        }
        // 末尾 "N 项"，整体 weight(1f) 推到最右；段数过多时前面的段省略
        Spacer(Modifier.weight(1f))
        Text(
            text = "$totalCount 项",
            style = t.caption,
            color = c.muted,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/* ───────────────────── Toolbar ───────────────────── */

@Composable
private fun Toolbar(
    activeType: FileType?,
    counts: TabCounts,
    viewMode: ViewMode,
    onTypeClick: (FileType?) -> Unit,
    onViewToggle: (ViewMode) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 6 个 tab 在窄屏（Medium Phone 360dp）上总宽必溢出。改 LazyRow 横向滚动
            // —— 对齐 design HTML 的 `overflow-x: auto`。contentPadding(20dp) 复刻
            // 原来的左右 20dp padding。spacedBy(4dp) 复刻原 `gap: 4px`。
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = true,
            ) {
                item("全部") {
                    TypeTab("全部", counts.all,   activeType == null,            onClick = { onTypeClick(null) })
                }
                item("视频") {
                    TypeTab("视频", counts.video, activeType == FileType.VIDEO,  onClick = { onTypeClick(FileType.VIDEO) })
                }
                item("图片") {
                    TypeTab("图片", counts.image, activeType == FileType.IMAGE,  onClick = { onTypeClick(FileType.IMAGE) })
                }
                item("文档") {
                    TypeTab("文档", counts.doc,   activeType == FileType.DOC,    onClick = { onTypeClick(FileType.DOC) })
                }
                item("音频") {
                    TypeTab("音频", counts.audio, activeType == FileType.AUDIO,  onClick = { onTypeClick(FileType.AUDIO) })
                }
                item("其他") {
                    TypeTab("其他", counts.other, activeType == FileType.OTHER,  onClick = { onTypeClick(FileType.OTHER) })
                }
            }
            ViewToggle(mode = viewMode, onToggle = onViewToggle)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = c.border, thickness = 1.dp)
    }
}

@Composable
private fun TypeTab(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = label,
                    style = t.body,
                    color = if (active) c.fg else c.muted,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    // 关键：禁换行 + 单行。design HTML 用 `white-space: nowrap`，
                    // 不禁的话窄屏 "其他" 会被竖向换行成"其/他"。
                    maxLines = 1,
                    softWrap = false,
                )
                Text(text = count.toString(), style = t.micro, color = Color(0xFFB0B0B0))
            }
            if (active) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(c.accent),
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ViewToggle(mode: ViewMode, onToggle: (ViewMode) -> Unit) {
    val c = StarVaultTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleBtn(active = mode == ViewMode.LIST, icon = Icons.ListView, onClick = { onToggle(ViewMode.LIST) })
        ToggleBtn(active = mode == ViewMode.GRID, icon = Icons.GridView, onClick = { onToggle(ViewMode.GRID) })
    }
}

@Composable
private fun ToggleBtn(active: Boolean, icon: ImageVector, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) c.fg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) c.surface else c.muted,
            modifier = Modifier.size(14.dp),
        )
    }
}

/* ───────────────────── SectionHead ───────────────────── */

@Composable
private fun SectionHead(visibleCount: Int, sortLabel: String, onSort: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "共 $visibleCount 项", style = t.caption, color = c.muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clickable(onClick = onSort),
        ) {
            Text(text = sortLabel, style = t.caption, color = c.fg)
            Icon(
                imageVector = Icons.ChevronDown,
                contentDescription = "排序",
                tint = c.fg,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/* ───────────────────── FileList ───────────────────── */

/**
 * 列表模式（设计 HTML `.file-row`）。
 *
 * 用 LazyColumn 替代原 Column+verticalScroll（之前全量渲染所有 row，100+ 项目录
 *  明显卡顿，且无法做滚动监听）。LazyColumn 有 item 复用 + 可选 LazyListState，
 *  后续要做 scroll-to-top / 隐藏 toolbar / infinite scroll 都直接接进去。
 *
 * 滚动加载更多（infinite scroll）：
 *  - rememberLazyListState 监听到 firstVisibleItemIndex + layoutInfo.totalItemsCount
 *  - 当"已看过的 item 数 + 阈值 ≥ 总数"且 hasMore=true 且没在加载中 → 调 onLoadMore()
 *  - 阈值 LOAD_MORE_THRESHOLD=4：用户距离底部还有 4 行时就触发，体验上滑到末尾时数据已就位
 *  - 底部 footer 用 item {} 加一个 32dp 的进度条（isLoadingMore=true 时显示；hasMore=false 时
 *    不显示，让列表直接结束）
 *
 *  - padding 保留：horizontal=12dp 让 FileListRow 的 8dp padding 与 design 一致；
 *                  bottom=80dp 避开 FAB
 *  - selection / open 回调语义不变
 */
@Composable
private fun FileList(
    files: List<FileEntry>,
    selectedIds: Set<String>,
    onSelect: (FileEntry) -> Unit,
    onOpen: (FileEntry) -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    // 滚动到接近底部时触发 loadMore。
    // 关键：用 hasMore/isLoadingMore 作 derivedStateOf 的 calculation key——这样当 hasMore
    // 或 isLoadingMore 变化（切目录 / loadMore 完成）时 State 会被重新生成，捕获到**当前**的
    // 值；否则老的 `remember { derivedStateOf { ... } }` 会固定捕获首次组合时的值，导致切目录后
    // 旧 hasMore=false 一直生效、新 hasMore=true 永远不生效（用户报的"只加载 50 条"就是这个）。
    // 同时把 listState 也作为 key：listState 本身在切目录时是同一个实例（rememberLazyListState
    // 只创建一次），但其内部 layoutInfo 仍是 State<T>，scroll 时自动触发 derivedStateOf 重算。
    val shouldLoadMore by remember(hasMore, isLoadingMore, listState) {
        derivedStateOf {
            if (!hasMore || isLoadingMore) return@derivedStateOf false
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            // lastVisible + threshold ≥ total ⇒ 接近底部
            lastVisible + LOAD_MORE_THRESHOLD >= total
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 80.dp),
    ) {
        items(
            items = files,
            key = { it.id },                  // 复用 key：同名文件 id 不同（fid vs cid），稳定
            contentType = { "FileRow" },  // 列表 pre-render 复用：相同 type 的 item 可共享 measure slot
        ) { f ->
            FileListRow(
                file = f,
                selected = f.id in selectedIds,
                onSelect = { onSelect(f) },
                onOpen = { onOpen(f) },
            )
        }
        // 列表底部加载指示器：仅在还能加载下一页且正在加载时显示
        if (isLoadingMore && hasMore) {
            item(key = "load_more_footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.size(width = 24.dp, height = 2.dp),
                    )
                }
            }
        }
    }
}

/** 距离底部 N 行时触发加载下一页；用户体验上"还没滚到底就已经在加载"。 */
private const val LOAD_MORE_THRESHOLD = 4

@Composable
private fun FileListRow(
    file: FileEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.accentSoft else Color.Transparent)
            .clickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CheckCircle(checked = selected, onClick = onSelect)
        FileThumb(type = file.type, size = 40, thumbnailUrl = file.thumbnailUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = file.metaSegments.joinToString(" · "),
                style = androidx.compose.ui.text.TextStyle(fontSize = 11.5.sp, fontFamily = t.caption.fontFamily),
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clickable { /* TODO: 单条更多 */ }
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.More,
                contentDescription = "更多",
                tint = c.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/* ───────────────────── FileGrid ───────────────────── */

/**
 * 网格模式（2 列），同样支持 infinite scroll。
 *
 * 用 LazyVerticalGrid + rememberLazyGridState，lastVisibleItem 与 totalItemsCount
 * 比较触发 loadMore。threshold 比列表模式大一点（6，因为每行 2 列）。
 */
@Composable
private fun FileGrid(
    files: List<FileEntry>,
    selectedIds: Set<String>,
    onSelect: (FileEntry) -> Unit,
    onOpen: (FileEntry) -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val c = StarVaultTheme.colors
    val gridState = rememberLazyGridState()
    // 关键：同 FileList 一样把 hasMore/isLoadingMore/gridState 放进 remember 的 key，
    // 否则切目录后旧 closure 捕获 stale hasMore=false，新 hasMore=true 永远不生效。
    val shouldLoadMore by remember(hasMore, isLoadingMore, gridState) {
        derivedStateOf {
            if (!hasMore || isLoadingMore) return@derivedStateOf false
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible + LOAD_MORE_THRESHOLD_GRID >= total
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(files, key = { it.id }, contentType = { "FileGridCell" }) { f ->
            GridItem(
                file = f,
                selected = f.id in selectedIds,
                onOpen = { onOpen(f) },
            )
        }
        // 网格底部加载指示器：跨整行 2 列
        if (isLoadingMore && hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "load_more_footer") {
                GridLoadingFooter()
            }
        }
    }
}

/** 单个网格项：选中边框 + 缩略图（叠加对勾）+ name + meta。 */
@Composable
private fun GridItem(
    file: FileEntry,
    selected: Boolean,
    onOpen: () -> Unit,
) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(
                width = 1.dp,
                color = if (selected) c.accent else c.border,
                shape = RoundedCornerShape(12.dp),
            )
            .background(if (selected) c.accentSoft else c.surface)
            .clickable(onClick = onOpen)
            .padding(8.dp),
    ) {
        Column {
            Box {
                FileThumb(type = file.type, size = 40, fillMaxWidth = true, thumbnailUrl = file.thumbnailUrl)
                if (selected) GridCheckBadge()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = file.name,
                style = t5(),
                color = c.fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = file.metaSegments.joinToString(" · "),
                style = androidx.compose.ui.text.TextStyle(fontSize = 10.5.sp, color = c.muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 网格项右上角的"已选"蓝底白勾徽标。 */
@Composable
private fun GridCheckBadge() {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(c.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Check,
            contentDescription = "已选",
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** 网格底部跨行的加载进度指示器。 */
@Composable
private fun GridLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(
            modifier = Modifier.size(width = 24.dp, height = 2.dp),
        )
    }
}

/** 网格模式距底部行数阈值：每行 2 列，6 项 = 3 行，提前量与列表相当。 */
private const val LOAD_MORE_THRESHOLD_GRID = 6

// StarVaultTheme.typography.body 替代 13sp
private fun t5() = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)

/* ───────────────────── FileThumb ───────────────────── */

/**
 * 文件缩略图（三态渲染，与 Home 屏 [com.starvault.component.FileRow] 内的 FileThumb 一致）。
 *
 * 无 URL 时的分支（按 [type] 区分）：
 *  - `FOLDER` → 灰色 FOLDER 渐变 + 白 Folder icon（folder 本来就没缩略图，渐变是设计意图）
 *  - 其他（IMAGE / VIDEO / AUDIO / DOC / ZIP / OTHER）→ `#FAFAFA` 灰底 + 灰色类型 icon
 *      （图片/视频"应该有缩略图但 115 没给 u" 跟"加载中"视觉一致）
 *
 * 有 URL 时（仅 IMAGE / VIDEO 实际会传 URL）：
 *  - 加载中 → `#FAFAFA` 灰底（loading slot 空，透出外层 Box）
 *  - 加载成功 → 真实缩略图（Crop）
 *  - 加载失败 → `#E4E4E7` 深灰底 + `#52525B` 裂图 icon
 *
 * 三态色阶：
 *   folder 无 URL:        `#6B7280 → #4B5563` 灰渐变 + 白 Folder icon
 *   其他无 URL / loading: `#FAFAFA` 灰底 + `#A1A1AA` icon（zinc-400）
 *   error:                `#E4E4E7` 深灰底 + `#52525B` icon（zinc-200 / zinc-600）
 *
 * @param size         列表模式下的边长（dp），默认 40
 * @param fillMaxWidth 网格模式：宽度撑满父布局，按 1:1 aspectRatio 渲染
 */
@Composable
private fun FileThumb(
    type: FileType,
    size: Int,
    fillMaxWidth: Boolean = false,
    thumbnailUrl: String? = null,
) {
    val brush = when (type) {
        FileType.FOLDER -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF6B7280), 1f to Color(0xFF4B5563)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.VIDEO  -> Brush.linearGradient(listOf(Color(0xFF2F6FEB), Color(0xFF1D4ED8)))
        FileType.IMAGE  -> Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C)))
        FileType.AUDIO  -> Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF7E22CE)))
        FileType.DOC    -> Brush.linearGradient(listOf(Color(0xFF16A34A), Color(0xFF15803D)))
        FileType.ZIP    -> Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFFBE185D)))
        FileType.OTHER  -> Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)))
    }
    val icon = when (type) {
        FileType.FOLDER -> Icons.Folder
        FileType.VIDEO  -> Icons.Play
        FileType.IMAGE  -> Icons.Image
        FileType.AUDIO  -> Icons.Music
        FileType.DOC    -> Icons.Doc
        FileType.ZIP    -> Icons.Archive
        FileType.OTHER  -> Icons.Folder
    }
    val shape = if (fillMaxWidth) RoundedCornerShape(8.dp) else RoundedCornerShape(9.dp)
    val baseMod = if (fillMaxWidth) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
    } else {
        Modifier
            .size(size.dp)
            .clip(shape)
    }

    if (thumbnailUrl.isNullOrBlank()) {
        if (type == FileType.FOLDER) {
            // ──── folder 无 URL：保留灰色 FOLDER 渐变 + 白 icon（设计意图）────
            ThumbBox(baseMod, brush, icon, fillMaxWidth, isFolder = true)
        } else {
            // ──── 其他类型无 URL：灰底 + 灰 icon（统一 loading 视觉）────
            ThumbBox(baseMod, brush = null, icon, fillMaxWidth, isFolder = false)
        }
        return
    }

    // ──── 有 URL：Coil 三态渲染（保留 SubcomposeAsyncImage 三 slot，仅加 size hint） ────
    //
    // 性能要点：SubcomposeAsyncImage 的 sub-compose 开销其实很小（一次重组 vs 没它多一层），
    // **真正的卡顿是 decode 时间**——没 size hint 时 Coil 解码原图（115 `_0` 档可能 1500×1500）
    // 再缩到 40dp，主线程 30ms+；传 size 后走 inSampleSize 采样，~3ms。
    //
    // 暂保留 SubcomposeAsyncImage 而非切到 AsyncImage+painter.state：后者要求把 Icon Box
    // 拆到 state 分支里（onError/onLoading 是非 Composable 回调，不能直接放 composable），
    // 改动面更大；size hint 这一项 ROI 最高，先单点切。
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) {
        size.dp.toPx().toInt()
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val request: ImageRequest = remember(thumbnailUrl, sizePx, fillMaxWidth) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .size(CoilSize(sizePx, sizePx))           // 关键：Coil 在 decode 时直接 downsample 到 40dp 像素
            .crossfade(120)                            // 平滑淡入，避免图 pop-in 触发 layout 抖动
            .memoryCacheKey("${thumbnailUrl}@${sizePx}")  // 不同尺寸独立缓存
            .build()
    }

    Box(modifier = baseMod.background(Color(0xFFFAFAFA))) {
        ThumbImage(request, fillMaxWidth)
    }
}

/**
 * 无 URL 时的缩略图 Box：folder 走渐变 + 白 icon；其他类型走灰底 + 灰 icon。
 */
@Composable
private fun ThumbBox(
    baseMod: Modifier,
    brush: Brush?,
    icon: ImageVector,
    fillMaxWidth: Boolean,
    isFolder: Boolean,
) {
    val mod = if (brush != null) baseMod.background(brush) else baseMod.background(Color(0xFFFAFAFA))
    val tint = if (isFolder) Color.White else Color(0xFFA1A1AA)
    Box(
        modifier = mod,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (fillMaxWidth) 28.dp else 18.dp),
        )
    }
}

/**
 * 有 URL 时的 Coil 异步缩略图，三态（loading / error / success）。
 */
@Composable
private fun ThumbImage(request: ImageRequest, fillMaxWidth: Boolean) {
    coil3.compose.SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        loading = {
            // loading slot 留空，外层 Box #FAFAFA 灰底透出
            ThumbLoading()
        },
        error = {
            ThumbError(fillMaxWidth)
        },
        success = { state ->
            SubcomposeAsyncImageContent()
        },
    )
}

/** 加载中占位（loading slot 留空透出外层灰底）。 */
@Composable
private fun ThumbLoading() {
    // intentionally empty — 外层 Box #FAFAFA 灰底透出
}

/** 加载失败占位：`#E4E4E7` 深灰底 + `#52525B` 裂图 icon。 */
@Composable
private fun ThumbError(fillMaxWidth: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE4E4E7)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.BrokenImage,
            contentDescription = "缩略图加载失败",
            tint = Color(0xFF52525B),
            modifier = Modifier.size(if (fillMaxWidth) 36.dp else 20.dp),
        )
    }
}

/* ───────────────────── CheckCircle ───────────────────── */

@Composable
private fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) c.accent else Color.Transparent)
            .border(
                width = 1.6.dp,
                color = if (checked) c.accent else Color(0xFFD0D0D0),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 5.dp)
                    .border(2.dp, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(1.dp)),
            )
        }
    }
}

/* ───────────────────── BulkBar ───────────────────── */

@Composable
private fun BulkBar(
    count: Int,
    onClose: () -> Unit,
    onAction: (BulkAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StarVaultTheme.colors.fg)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Close,
                contentDescription = "取消",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "已选 ",
                style = StarVaultTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = count.toString(),
                style = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = " 项",
                style = StarVaultTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        BulkActBtn(icon = Icons.Download, onClick = { onAction(BulkAction.DOWNLOAD) })
        BulkActBtn(icon = Icons.Share,    onClick = { onAction(BulkAction.SHARE) })
        BulkActBtn(icon = Icons.Move,     onClick = { onAction(BulkAction.MOVE) })
        BulkActBtn(icon = Icons.Rename,   onClick = { onAction(BulkAction.RENAME) })
        BulkActBtn(icon = Icons.Trash,    danger = true, onClick = { onAction(BulkAction.DELETE) })
    }
}

@Composable
private fun BulkActBtn(icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) Color(0xFFFF8A8A) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/* ───────────────────── FAB ───────────────────── */

@Composable
private fun Fab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(StarVaultTheme.colors.fg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Plus,
            contentDescription = "新建",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
