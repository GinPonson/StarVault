package com.starvault.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.starvault.component.Icons
import com.starvault.data.model.FileType
import com.starvault.theme.StarVaultTheme

/**
 * Files 屏（对应 design/06-files.html 的"我的文件" 1:1 复刻）。
 *
 * 整体结构（垂直滚动 / 网格）：
 *   ┌─ AppBar       "我的文件" + 4 icon-btn（搜索 / 传输 / 更多 / 新建）
 *   ├─ Crumb        "我的文件" + "/" + "28 项" right
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
    onCloseBulk: () -> Unit = {},
    onBulkAction: (BulkAction) -> Unit = {},
    onUpload: () -> Unit = {},
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
                    Crumb(totalCount = state.all.size)
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
                            )
                            ViewMode.GRID -> FileGrid(
                                files = visible(state),
                                selectedIds = state.selectedIds,
                                onSelect = onSelect,
                                onOpen = onOpen,
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

@Composable
private fun Crumb(totalCount: Int) {
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
        Text(text = "我的文件", style = t.caption, color = c.fg, fontWeight = FontWeight.SemiBold)
        Text(text = "/", style = t.caption, color = Color(0xFFC4C4C4))
        Text(
            text = "$totalCount 项",
            style = t.caption,
            color = c.muted,
            modifier = Modifier.weight(1f),
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
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TypeTab("全部", counts.all,   activeType == null,            onClick = { onTypeClick(null) })
                TypeTab("视频", counts.video, activeType == FileType.VIDEO,  onClick = { onTypeClick(FileType.VIDEO) })
                TypeTab("图片", counts.image, activeType == FileType.IMAGE,  onClick = { onTypeClick(FileType.IMAGE) })
                TypeTab("文档", counts.doc,   activeType == FileType.DOC,    onClick = { onTypeClick(FileType.DOC) })
                TypeTab("音频", counts.audio, activeType == FileType.AUDIO,  onClick = { onTypeClick(FileType.AUDIO) })
                TypeTab("其他", counts.other, activeType == FileType.OTHER,  onClick = { onTypeClick(FileType.OTHER) })
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

@Composable
private fun FileList(
    files: List<FileEntry>,
    selectedIds: Set<String>,
    onSelect: (FileEntry) -> Unit,
    onOpen: (FileEntry) -> Unit,
) {
    val c = StarVaultTheme.colors
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp)
            .padding(bottom = 80.dp),
    ) {
        files.forEach { f ->
            FileListRow(
                file = f,
                selected = f.id in selectedIds,
                onSelect = { onSelect(f) },
                onOpen = { onOpen(f) },
            )
        }
    }
}

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

@Composable
private fun FileGrid(
    files: List<FileEntry>,
    selectedIds: Set<String>,
    onSelect: (FileEntry) -> Unit,
    onOpen: (FileEntry) -> Unit,
) {
    val c = StarVaultTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(files, key = { it.id }) { f ->
            val sel = f.id in selectedIds
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface)
                    .border(
                        width = 1.dp,
                        color = if (sel) c.accent else c.border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(if (sel) c.accentSoft else c.surface)
                    .clickable(onClick = { onOpen(f) })
                    .padding(8.dp),
            ) {
                Column {
                    Box {
                        FileThumb(type = f.type, size = 40, fillMaxWidth = true, thumbnailUrl = f.thumbnailUrl)
                        if (sel) {
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
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = f.name,
                        style = t5(),
                        color = c.fg,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = f.metaSegments.joinToString(" · "),
                        style = androidx.compose.ui.text.TextStyle(fontSize = 10.5.sp, color = c.muted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// StarVaultTheme.typography.body 替代 13sp
private fun t5() = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)

/* ───────────────────── FileThumb ───────────────────── */

/**
 * 文件缩略图：渐变色块背景 + icon。IMAGE / VIDEO 若 [thumbnailUrl] 非空，用 Coil AsyncImage
 * 加载 115 缩略图（响应 `u` 字段）覆盖在背景上。
 */
@Composable
private fun FileThumb(
    type: FileType,
    size: Int,
    fillMaxWidth: Boolean = false,
    thumbnailUrl: String? = null,
) {
    val brush = when (type) {
        FileType.FOLDER -> Brush.linearGradient(listOf(Color(0xFF6B7280), Color(0xFF4B5563)))
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
    val mod = if (fillMaxWidth) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(brush)
    } else {
        Modifier
            .size(size.dp)
            .clip(shape)
            .background(brush)
    }
    Box(modifier = mod, contentAlignment = Alignment.Center) {
        // IMAGE / VIDEO 缩略图覆盖在背景上（失败 fallback 到 icon）
        if (!thumbnailUrl.isNullOrBlank()) {
            coil3.compose.AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (fillMaxWidth) 28.dp else 18.dp),
            )
        }
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
