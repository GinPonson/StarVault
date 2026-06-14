package com.starvault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.starvault.component.FileRow
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

/**
 * Home 屏（对应 design/01-home.html 的完整结构 1:1 复刻）。
 *
 * 整体结构（自上而下）：
 *   ┌─ AppBar           22sp "首页" + 3 个 40dp icon button
 *   ├─ Quick grid       1fr×4：最近/收藏/传输（带红点）/回收站
 *   ├─ TagStrip         横向滚动：全部(active) + 5 色 tag + 新建
 *   ├─ SectionHead      "我的文件" 大写 muted + "排序：最近 ▾"
 *   ├─ FileList         6 条 FileRow（按 activeTag 过滤）
 *   └─ FAB              56dp fg 圆 + "+"，叠在文件列表之上
 *
 * 状态映射（与 [HomeUiState] 一一对应）：
 *   - Loading  : 不渲染文件区（顶部 4 段照常）
 *   - Success  : 完整渲染；按 activeTag 过滤 fileList
 *   - Error    : 顶部 4 段 + 文件区显示 message
 *
 * @param state              当前 UI 状态
 * @param onTagClick         tag-strip 上的具名 tag 点击（含 "新建" 通过 label=="新建" 区分）
 * @param onAllTagClick      tag-strip 上的 "全部" chip 点击
 * @param onSortClick        section head 右侧 "排序：最近 ▾"
 * @param onFabClick         底部 FAB
 * @param onFileClick        file row 点击
 * @param onFileMore         file row ⋯ 点击
 * @param onQuickClick       4 个快捷入口点击
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTagClick: (HomeQuickTag) -> Unit,
    onAllTagClick: () -> Unit,
    onSortClick: () -> Unit,
    onFabClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileMore: (FileItem) -> Unit,
    onQuickClick: (HomeQuick) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    Box(modifier = modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeAppBar()
            QuickGrid(onClick = onQuickClick)
            TagStrip(
                active = state.activeTag,
                onTagClick = onTagClick,
                onAllTagClick = onAllTagClick,
            )
            SectionHead(onSortClick = onSortClick)
            FileList(
                state = state,
                onFileClick = onFileClick,
                onFileMore = onFileMore,
            )
        }
        // FAB 浮在最右下方（叠在 Column 之上）
        HomeFab(onClick = onFabClick, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/* ───────────────────────────── AppBar ───────────────────────────── */

/**
 * 顶部 AppBar：左侧 22sp bold "首页"，右侧 3 个 40dp 圆形 icon-btn。
 *
 *  icon 占位：Phase 1 用 unicode glyph 近似（Valkyrie 上线后替换为真实 SVG）：
 *   - 扫描  ⌖    (U+2316 position indicator)
 *   - 通知  ◉    (U+25C9 fisheye, 类通知铃)
 *   - 更多  ⋯    (与 FileRow 一致)
 */
@Composable
private fun HomeAppBar() {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "首页",
            style = t.large,           // 22sp SemiBold，与 HTML .app-title 一致
            color = c.fg,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconGlyph("⌖", "扫描")
            IconGlyph("◉", "通知")
            IconGlyph("⋯", "更多")
        }
    }
}

/** 40dp 圆形可点 icon 占位（hover bg 在 Phase 1 静态截图里不需要）。*/
@Composable
private fun IconGlyph(glyph: String, contentDescription: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { /* stub */ },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = t.subtitle,        // 16sp
            color = c.fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ───────────────────────────── Quick Grid ───────────────────────────── */

/** 4 个快捷入口：最近/收藏/传输（带红点）/回收站。*/
enum class HomeQuick(val label: String, val glyph: String, val showRecordDot: Boolean = false) {
    RECENT("最近",  "◴"),                          // 时钟
    STAR("收藏",   "★"),                          // 星
    TRANSFERS("传输", "↑", showRecordDot = true),  // 上箭头 + 红点（与 HTML 一致）
    TRASH("回收站", "↻"),                          // 顺时针回转
}

@Composable
private fun QuickGrid(onClick: (HomeQuick) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HomeQuick.entries.forEach { q ->
            QuickCell(
                quick = q,
                modifier = Modifier.weight(1f),
                onClick = { onClick(q) },
            )
        }
    }
}

@Composable
private fun QuickCell(quick: HomeQuick, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = quick.glyph,
                style = t.subtitle,    // 16sp
                color = c.fg,
            )
            // 传输入口：右上角 8dp 红点（HTML `.dot-rec::after`）
            if (quick.showRecordDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.danger)
                        .border(2.dp, c.surface, CircleShape),
                )
            }
        }
        Text(
            text = quick.label,
            style = t.caption,        // 12sp
            color = c.fg,
        )
    }
}

/* ───────────────────────────── Tag Strip ───────────────────────────── */

/**
 * 横向滚动 tag-strip：
 *  - "全部" chip：active 时 fg 底白字 + ✓ icon；非 active 时 surface 底 + 边框
 *  - 5 个具名 tag：surface 底 + 边框 + 8dp 色点（对应 tag1..5）
 *  - "新建" chip：surface 底 + 边框 + "+" icon
 */
@Composable
private fun TagStrip(
    active: FileTag?,
    onTagClick: (HomeQuickTag) -> Unit,
    onAllTagClick: () -> Unit,
) {
    val c = StarVaultTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1) "全部"
        AllChip(isActive = active == null, onClick = onAllTagClick)
        // 2) 5 个具名 tag
        HomeQuickTag.entries.forEach { tag ->
            NamedTagChip(
                tag = tag,
                isActive = active?.label == tag.label,
                onClick = { onTagClick(tag) },
            )
        }
        // 3) "新建"
        NewTagChip(onClick = onAllTagClick /* stub */)
    }
}

@Composable
private fun AllChip(isActive: Boolean, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isActive) c.fg else c.surface)
            .border(
                1.dp,
                if (isActive) c.fg else c.border,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isActive) {
            Text(text = "✓", style = t.caption, color = c.accentOn)
        }
        Text(
            text = "全部",
            style = t.caption,    // 12.5sp 视觉感
            color = if (isActive) c.accentOn else c.fg,
        )
    }
}

@Composable
private fun NamedTagChip(tag: HomeQuickTag, isActive: Boolean, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isActive) c.fg else c.surface)
            .border(
                1.dp,
                if (isActive) c.fg else c.border,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 8dp 色点（与 design HTML `.swatch` 视觉一致）
        val swatchColor = tagSwatchColor(tag.color)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(swatchColor),
        )
        Text(
            text = tag.label,
            style = t.caption,
            color = if (isActive) c.accentOn else c.fg,
        )
    }
}

@Composable
private fun NewTagChip(onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "+", style = t.caption, color = c.fg)
        Text(text = "新建", style = t.caption, color = c.fg)
    }
}

/** 把 TagColor 映射回 5 色 hex，与 design 的 --tag-1..5 完全一致。*/
private fun tagSwatchColor(color: TagColor): Color = when (color) {
    TagColor.TAG1 -> Color(0xFF2F6FEB)
    TagColor.TAG2 -> Color(0xFF9333EA)
    TagColor.TAG3 -> Color(0xFFEA580C)
    TagColor.TAG4 -> Color(0xFF16A34A)
    TagColor.TAG5 -> Color(0xFFDB2777)
}

/* ───────────────────────────── Section Head ───────────────────────────── */

/**
 * 段头：左 muted uppercase 13sp "我的文件"，右 muted 12sp "排序：最近 ▾"。
 */
@Composable
private fun SectionHead(onSortClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // HTML: 13sp, 600, letter-spacing 0.04em, uppercase, muted
        Text(
            text = "我的文件",
            style = t.caption.copy(letterSpacing = 0.04.em),
            color = c.muted,
        )
        Text(
            text = "排序：最近 ▾",
            style = t.caption,
            color = c.muted,
            modifier = Modifier.clickable(onClick = onSortClick),
        )
    }
}

/* ───────────────────────────── File List ───────────────────────────── */

/**
 * 文件列表：
 *  - Success 且 files 非空 → 逐条 FileRow，meta 第二段走 relTimes[id]
 *  - Success 但 activeTag 过滤后空 → 渲染空提示（"该分类下暂无文件"）
 *  - Loading → 渲染 0 行
 *  - Error   → 渲染 message 文本
 */
@Composable
private fun FileList(
    state: HomeUiState,
    onFileClick: (FileItem) -> Unit,
    onFileMore: (FileItem) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    // 按 activeTag 过滤；null = 全部
    // 提前把 activeTag 拉成 val 避免 sealed interface 的 open getter 触发 smart cast 失败
    val activeTag = state.activeTag
    val filtered = remember(state) {
        if (activeTag == null) state.files
        else state.files.filter { it.tag?.label == activeTag.label }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        when (state) {
            is HomeUiState.Success -> {
                if (filtered.isEmpty()) {
                    Text(
                        text = if (state.activeTag == null) "暂无文件" else "该分类下暂无文件",
                        style = t.body,
                        color = c.muted,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    filtered.forEach { f ->
                        FileRow(
                            file = f,
                            onClick = { onFileClick(f) },
                            onMore = { onFileMore(f) },
                            metaSecondaryOverride = state.relTimes[f.id],
                        )
                    }
                }
            }
            is HomeUiState.Loading -> Unit   // 占位不渲染
            is HomeUiState.Error -> {
                Text(
                    text = state.message,
                    style = t.body,
                    color = c.danger,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        Spacer(Modifier.height(80.dp))   // 给 FAB 留空间
    }
}

/* ───────────────────────────── FAB ───────────────────────────── */

/**
 * 56dp 黑色 FAB（HTML `.fab`）：右下角 + box-shadow。
 *
 *  - box-shadow 在 Compose 用 [Box] 嵌套 2 层（半透明大 blur 底层 + 实体 fg 顶层）模拟
 *  - "+" 用 subtitle 16sp，视觉上居中
 */
@Composable
private fun HomeFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = modifier
            .padding(end = 20.dp, bottom = 24.dp)
            .size(56.dp),
    ) {
        // 阴影层：56+12=68dp，居中后下偏移
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.18f)),
        )
        // 实体层
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .clip(CircleShape)
                .background(c.fg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", style = t.title, color = c.accentOn)   // 20sp
        }
    }
}

/* ───────────────────────────── 工具扩展 ───────────────────────────── */

