package com.starvault.ui.album

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starvault.theme.StarVaultTheme

/**
 * Album 屏（对应 design/07-album.html 的"相册" 1:1 复刻）。
 *
 * 整体结构（垂直滚动）：
 *   ┌─ AppBar         "相册" + 3 icon-btn（搜索/相机/更多）
 *   ├─ FolderPicker   圆角 pill "我的相册 ▾" + stats "1,247 张 · 23 个视频 · 4.2 GB"
 *   ├─ SubTabs        4 圆角 chip（全部/收藏/最近上传/原图）
 *   └─ Recent
 *       ├─ SectionHead "最近" + "查看全部"
 *       └─ DateGroup × 4（3 列网格；每组顶部日期 label）
 *   +  FolderSheet（ModalBottomSheet 1:1：相册列表 + 子目录 + 最近 + 新建 + 取消）
 *   +  SwitchOverlay（圆点 spinner 遮罩）
 *
 * 状态映射（与 [AlbumUiState] 对应）：
 *   - Loading : 占位
 *   - Success : 完整渲染 + 弹 sheet
 *   - Error   : message
 */
@Composable
fun AlbumScreen(
    state: AlbumUiState,
    onSearch: () -> Unit = {},
    onCamera: () -> Unit = {},
    onMore: () -> Unit = {},
    onOpenSheet: () -> Unit = {},
    onSubTab: (SubTabId) -> Unit = {},
    onSeeAll: () -> Unit = {},
    onPhotoClick: (PhotoEntry) -> Unit = {},
    onSelectAlbum: (String) -> Unit = {},
    onCloseSheet: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    Box(modifier = modifier.fillMaxSize().background(c.bg)) {
        when (state) {
            is AlbumUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", style = StarVaultTheme.typography.body, color = c.muted)
                }
            }
            is AlbumUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, style = StarVaultTheme.typography.body, color = c.danger)
                }
            }
            is AlbumUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AppBar(onSearch, onCamera, onMore)
                    FolderPicker(
                        album = state.currentAlbum,
                        onClick = onOpenSheet,
                    )
                    SubTabs(
                        tabs = state.subTabs,
                        active = state.activeSubTab,
                        onClick = onSubTab,
                    )
                    RecentSection(
                        groups = state.dateGroups,
                        onSeeAll = onSeeAll,
                        onPhotoClick = onPhotoClick,
                    )
                    Spacer(Modifier.height(40.dp))
                }

                // Modal bottom sheet
                if (state.folderSheet is FolderSheetState.Open) {
                    val sheet = state.folderSheet
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(onClick = onCloseSheet),
                    )
                    FolderSheet(
                        albums = sheet.albums,
                        childrenOf = sheet.childrenOf,
                        recents = sheet.recents,
                        currentId = sheet.currentId,
                        onSelect = onSelectAlbum,
                        onNew = onNewFolder,
                        onClose = onCloseSheet,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/* ───────────────────── AppBar ───────────────────── */

@Composable
private fun AppBar(onSearch: () -> Unit, onCamera: () -> Unit, onMore: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "相册", style = t.large, color = c.fg)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconBtn(glyph = "⌕", onClick = onSearch,  contentDescription = "搜索")
            IconBtn(glyph = "◉", onClick = onCamera,  contentDescription = "相机")
            IconBtn(glyph = "⋯", onClick = onMore,    contentDescription = "更多")
        }
    }
}

@Composable
private fun IconBtn(glyph: String, onClick: () -> Unit, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = StarVaultTheme.typography.subtitle, color = StarVaultTheme.colors.fg)
    }
}

/* ───────────────────── FolderPicker ───────────────────── */

@Composable
private fun FolderPicker(album: AlbumFolder, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .padding(start = 8.dp, end = 10.dp)
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("▢", style = t.caption, color = c.accent)
            Text(album.name, style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold), color = c.fg)
            Text("▾", style = TextStyle(fontSize = 12.sp), color = c.muted)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = album.photoCount.toString() + " 张照片",
                style = TextStyle(fontSize = 11.sp, color = c.muted, fontWeight = FontWeight.Medium),
            )
            Text("·", style = t.micro, color = Color(0xFFC4C4C4))
            Text(
                text = "${album.videoCount} 个视频",
                style = TextStyle(fontSize = 11.sp, color = c.muted, fontWeight = FontWeight.Medium),
            )
            Text("·", style = t.micro, color = Color(0xFFC4C4C4))
            Text(album.totalGb, style = TextStyle(fontSize = 11.sp, color = c.muted, fontWeight = FontWeight.Medium))
        }
    }
}

/* ───────────────────── SubTabs ───────────────────── */

@Composable
private fun SubTabs(
    tabs: List<SubTab>,
    active: SubTabId,
    onClick: (SubTabId) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == active
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isActive) c.fg else c.surface)
                    .border(1.dp, if (isActive) c.fg else c.border, RoundedCornerShape(999.dp))
                    .clickable { onClick(tab.id) }
                    .height(28.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = if (isActive) c.surface else c.muted,
                )
            }
        }
    }
}

/* ───────────────────── RecentSection ───────────────────── */

@Composable
private fun RecentSection(
    groups: List<DateGroup>,
    onSeeAll: () -> Unit,
    onPhotoClick: (PhotoEntry) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "最近", style = t.title, color = c.fg)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clickable(onClick = onSeeAll),
            ) {
                Text(text = "查看全部", style = t.caption, color = c.muted)
                Text("›", style = t.caption, color = c.muted)
            }
        }
        groups.forEach { g ->
            DateGroupView(group = g, onPhotoClick = onPhotoClick)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DateGroupView(group: DateGroup, onPhotoClick: (PhotoEntry) -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = group.label, style = t.micro, color = c.muted)
            if (group.subLabel != null) {
                Text(
                    text = "· ${group.subLabel}",
                    style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                    color = c.fg,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            group.photos.forEach { p ->
                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    PhotoCell(photo = p, onClick = { onPhotoClick(p) })
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(photo: PhotoEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(sceneBrush(photo.scene))
            .clickable(onClick = onClick),
    ) {
        when (photo.kind) {
            PhotoKind.VIDEO -> {
                // 半透黑 + 居中 ▶
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", style = TextStyle(fontSize = 28.sp, color = Color.White))
                }
            }
            PhotoKind.SCREENSHOT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F4))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(30.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFF2F6FEB)),
                    )
                    Box(modifier = Modifier.height(3.dp).fillMaxWidth(0.88f).background(Color(0xFFD0D0D5), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.height(3.dp).fillMaxWidth(0.70f).background(Color(0xFFD0D0D5), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.height(3.dp).fillMaxWidth(0.92f).background(Color(0xFFD0D0D5), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.height(3.dp).fillMaxWidth(0.55f).background(Color(0xFFD0D0D5), RoundedCornerShape(1.dp)))
                }
            }
            PhotoKind.DOCUMENT -> {
                // 渐变已在 background 上，"PDF" 标签
            }
            PhotoKind.PHOTO -> Unit
        }
        // time / kind label
        if (photo.timeLabel != null) {
            Text(
                text = photo.timeLabel,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 4.dp),
            )
        }
        // fav
        if (photo.isFavorite) {
            Text(
                text = "♥",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFFEF4444)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 5.dp, top = 5.dp),
            )
        }
    }
}

@Composable
private fun sceneBrush(scene: PhotoScene): Brush = when (scene) {
    PhotoScene.SUNSET -> Brush.linearGradient(
        listOf(Color(0xFFFF7B5C), Color(0xFFC0306B), Color(0xFF4A148C)),
    )
    PhotoScene.FOREST -> Brush.linearGradient(
        listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF52B788)),
    )
    PhotoScene.MOUNTAIN -> Brush.linearGradient(
        listOf(Color(0xFF6B8AAE), Color(0xFF3B5278), Color(0xFF1A2B4A)),
    )
    PhotoScene.COFFEE -> Brush.linearGradient(
        listOf(Color(0xFFC9A876), Color(0xFF6B4423)),
    )
    PhotoScene.BLDG -> Brush.linearGradient(
        listOf(Color(0xFFB8C5D6), Color(0xFF5C6F87)),
    )
    PhotoScene.NIGHT -> Brush.linearGradient(
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF4A4A8A)),
    )
    PhotoScene.FOOD -> Brush.linearGradient(
        listOf(Color(0xFFF6D365), Color(0xFFFDA085)),
    )
    PhotoScene.PORTRAIT -> Brush.linearGradient(
        listOf(Color(0xFFF8B195), Color(0xFFC06C84)),
    )
    PhotoScene.BEACH -> Brush.linearGradient(
        listOf(Color(0xFF87CEEB), Color(0xFF5BADD6), Color(0xFF1E5C8A)),
    )
    PhotoScene.DOC -> Brush.linearGradient(
        listOf(Color(0xFFFAFAFA), Color(0xFFD8D8DC)),
    )
}

/* ───────────────────── FolderSheet ───────────────────── */

@Composable
private fun FolderSheet(
    albums: List<AlbumFolder>,
    childrenOf: Map<String, List<AlbumFolder>>,
    recents: List<AlbumFolder>,
    currentId: String,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(c.surface)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        // grabber
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFC4C4C4)),
        )
        Spacer(Modifier.height(20.dp))
        // title
        Text(
            text = "选择相册",
            style = t.title,
            color = c.fg,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 16.dp),
        )
        // scroll list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            albums.forEach { a ->
                val isCurrent = a.id == currentId
                val children = childrenOf[a.id].orEmpty()
                val hasChildren = children.isNotEmpty()
                val hasCurrentChild = children.any { it.id == currentId }
                val isInScope = isCurrent || hasCurrentChild
                FolderRow(
                    name = a.name,
                    color = a.color,
                    isCurrent = isInScope,
                    showCheck = isInScope,
                    showChev = hasChildren,
                    indentDp = 0,
                    onClick = { onSelect(a.id) },
                )
                if (hasChildren && isInScope) {
                    children.forEach { child ->
                        FolderRow(
                            name = child.name,
                            color = child.color,
                            isCurrent = child.id == currentId,
                            showCheck = child.id == currentId,
                            showChev = false,
                            indentDp = 24,
                            onClick = { onSelect(child.id) },
                        )
                    }
                }
            }
            HorizontalDivider(color = c.border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            Text(
                text = "最近使用",
                style = t.micro,
                color = c.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recents.forEach { r ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.bg)
                            .border(1.dp, c.border, RoundedCornerShape(999.dp))
                            .clickable { onSelect(r.id) }
                            .height(32.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(r.color),
                        )
                        Text(r.name, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = c.fg)
                    }
                }
            }
            HorizontalDivider(color = c.border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNew)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("+", style = TextStyle(fontSize = 16.sp, color = c.accent, fontWeight = FontWeight.SemiBold))
                Text("新建相册", style = t.body, color = c.accent)
            }
        }
        // cancel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(c.bg)
                .clickable(onClick = onClose)
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("取消", style = t.body, color = c.accent)
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    color: Color,
    isCurrent: Boolean,
    showCheck: Boolean,
    showChev: Boolean,
    indentDp: Int,
    onClick: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val padStart = 24 + indentDp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = padStart.dp, end = 24.dp, top = 0.dp, bottom = 0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text("▢", style = TextStyle(fontSize = 14.sp, color = Color.White))
        }
        Text(
            text = name,
            style = t.body,
            color = c.fg,
            modifier = Modifier.weight(1f),
        )
        if (showChev) Text("›", style = t.subtitle, color = c.muted)
        if (showCheck) Text("✓", style = t.subtitle, color = c.accent, fontWeight = FontWeight.SemiBold)
    }
}
