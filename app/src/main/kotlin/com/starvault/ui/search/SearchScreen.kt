package com.starvault.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starvault.component.Icons
import com.starvault.data.model.FileType
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.files.FileEntry

/**
 * 搜索屏（全屏）。
 *
 *  视觉（对齐 Files 屏风格）：
 *  - 顶栏：← 返回 icon + OutlinedTextField 搜索框（自动获焦 + 弹键盘） + X 清空按钮
 *  - 结果区：
 *    - Idle/空 query → 居中提示「输入文件名搜索」
 *    - Searching → 顶部 LinearProgressIndicator + "搜索中..." 文字
 *    - Success → LazyColumn 渲染 FileEntry（复用 FilesScreen 行布局）
 *    - Empty → 居中「未找到结果」
 *    - Error → 居中「错误: $message」+ 返回按钮
 *  - 底栏：BottomNavBar（暂不渲染 — 搜索是 Files 屏的子页面，全屏覆盖）
 *
 *  自动获焦：进屏即弹键盘，OutlinedTextField.requestFocus()。
 *
 *  滚动到底加载更多：Success 列表检测到滚到底 → vm.loadMore()。
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (FileEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography

    BackHandler(enabled = true) { onBack() }

    Column(modifier = modifier.fillMaxSize().background(c.bg)) {
        // ───── 顶栏：返回 + 搜索框 + X ─────
        SearchAppBar(
            query = state.query,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            onBack = onBack,
        )

        // ───── 内容区 ─────
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is SearchUiState.Idle -> IdleHint(query = state.query)
                is SearchUiState.Searching -> SearchingBlock(query = state.query)
                is SearchUiState.Success -> ResultList(
                    state = state,
                    onLoadMore = onLoadMore,
                    onOpen = onOpen,
                )
                is SearchUiState.Empty -> EmptyHint(query = state.query)
            }
        }
    }
}

/* ─────────────────── 顶栏 ─────────────────── */

@Composable
private fun SearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 返回
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Back,
                contentDescription = "返回",
                tint = c.fg,
                modifier = Modifier.size(22.dp),
            )
        }

        // 搜索框
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("搜索文件名", color = c.muted, fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Search,
                    contentDescription = null,
                    tint = c.muted,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearQuery),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Close,
                            contentDescription = "清空",
                            tint = c.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.bg,
                unfocusedContainerColor = c.bg,
                focusedIndicatorColor = c.border,
                unfocusedIndicatorColor = c.border,
                cursorColor = c.accent,
                focusedTextColor = c.fg,
                unfocusedTextColor = c.fg,
            ),
            shape = RoundedCornerShape(24.dp),
        )
    }
}

/* ─────────────────── 状态块 ─────────────────── */

@Composable
private fun IdleHint(query: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) "输入文件名搜索" else "搜索中…",
            style = t.body,
            color = c.muted,
        )
    }
}

@Composable
private fun SearchingBlock(query: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = c.accent,
            trackColor = c.border,
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "搜索 \"$query\" 中…",
                style = t.body,
                color = c.muted,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

@Composable
private fun EmptyHint(query: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "未找到 \"$query\" 相关结果",
                style = t.body,
                color = c.muted,
            )
        }
    }
}

/* ─────────────────── 结果列表（复用 FilesScreen 行布局风格） ─────────────────── */

/**
 * 搜索结果 LazyColumn（无限滚动）。
 *
 *  与 FilesScreen.FileList 同款：
 *  - 用 derivedStateOf 检测滚到底 → vm.loadMore()
 *  - 列表底部小进度条（isLoadingMore + hasMore）
 *  - 每行：thumb + name + metaSegments（缩略图走 FileEntry.thumbnailUrl）
 */
@Composable
private fun ResultList(
    state: SearchUiState.Success,
    onLoadMore: () -> Unit,
    onOpen: (FileEntry) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.hasMore, state.isLoadingMore, listState) {
        derivedStateOf {
            if (!state.hasMore || state.isLoadingMore) return@derivedStateOf false
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible + 4 >= total
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    val c = StarVaultTheme.colors

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        items(state.results, key = { it.id }) { entry ->
            ResultRow(entry = entry, onClick = { onOpen(entry) })
        }
        if (state.isLoadingMore && state.hasMore) {
            items(1, key = { "load_more_footer" }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.size(width = 24.dp, height = 2.dp),
                        color = c.accent,
                        trackColor = c.border,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(entry: FileEntry, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 简化版缩略图：folder / 其它类型走 type icon；IMAGE / VIDEO 有 thumbnailUrl 时由 ResultThumb 显示
        ResultThumb(type = entry.type, thumbnailUrl = entry.thumbnailUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = entry.metaSegments.joinToString(" · "),
                style = androidx.compose.ui.text.TextStyle(fontSize = 11.5.sp, fontFamily = t.caption.fontFamily),
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 简化版缩略图（搜屏专用，避免依赖 FilesScreen 内部的 FileThumb private）。
 *
 *  - folder / 其它类型 → 灰底 + type icon
 *  - IMAGE / VIDEO 有 thumbnailUrl → 灰底 + coil 图（lazy loading）
 *  - 失败不重试，灰底占位
 */
@Composable
private fun ResultThumb(type: FileType, thumbnailUrl: String?) {
    val c = StarVaultTheme.colors
    val placeholderGray = Color(0xFFFAFAFA)
    val placeholderIconTint = Color(0xFFA1A1AA)
    val (icon, _) = when (type) {
        FileType.FOLDER -> Icons.Files to Color.White
        FileType.IMAGE -> Icons.Album to Color.White
        FileType.VIDEO -> Icons.Cast to Color.White
        FileType.AUDIO -> Icons.Cast to Color.White
        // DOC / ZIP / OTHER 跟 Home / Files 屏 thumbStyle 对齐 Solar Bold 系列
        // (后者已用 Solar DocumentText / ZipFile / File;placeholder 统一 zinc 灰底)
        FileType.DOC -> Icons.Doc to Color.White
        FileType.ZIP -> Icons.Archive to Color.White
        FileType.OTHER -> Icons.GenericFile to Color.White
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(placeholderGray),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = placeholderIconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}