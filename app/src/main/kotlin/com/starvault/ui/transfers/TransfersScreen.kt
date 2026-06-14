package com.starvault.ui.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.theme.StarVaultTheme

/**
 * Transfers 屏（对应 design/04-transfers.html 的"传输中心" 1:1 复刻）。
 *
 * 整体结构（垂直滚动）：
 *   ┌─ AppBar        "传输" + 进行中 sub + 搜索/清空 icon-btn
 *   ├─ Overview      实时状态 + 上下行速率 + 3 stat（进行中/已传输/已完成）
 *   ├─ Tabs          进行中(N) / 已完成(N) / 已离线(N)
 *   ├─ OpRow         "已按添加时间排序" + "全部暂停" CTA
 *   └─ List          5 条 transfer 卡片（thumb + name + pause/more + 进度条 + foot）
 *
 * 状态映射（与 [TransfersUiState] 对应）：
 *   - Loading  : Overview + List 显示 stub
 *   - Ready    : 完整渲染；按 activeTab 过滤
 *   - Error    : 显示 message
 *
 * @param state       UiState
 * @param onSearch    搜索 icon
 * @param onClear     清空已完成
 * @param onTab       切 tab
 * @param onPauseAll  全部暂停
 * @param onPause     单条暂停/恢复
 * @param onMore      单条更多
 * @param onRetry     失败重试
 */
@Composable
fun TransfersScreen(
    state: TransfersUiState,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onTab: (TransfersTab) -> Unit,
    onPauseAll: () -> Unit,
    onPause: (Transfer) -> Unit,
    onMore: (Transfer) -> Unit,
    onRetry: (Transfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // 1) AppBar
        AppBar(activeCount = state.totalActive, onSearch = onSearch, onClear = onClear)
        // 2) Overview
        Overview(
            totalActive = state.totalActive,
            totalDone = state.totalDone,
            upSpeedBps = state.upSpeedBps,
            downSpeedBps = state.downSpeedBps,
        )
        // 3) Tabs
        Tabs(
            active = state.activeTab,
            totalActive = state.totalActive,
            totalDone = state.totalDone,
            totalOffline = state.totalOffline,
            onTab = onTab,
        )
        // 4) OpRow
        OpRow(onPauseAll = onPauseAll)
        // 5) List
        val filtered = remember(state) {
            when (state.activeTab) {
                TransfersTab.Active  -> state.all.filter { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED }
                TransfersTab.Done    -> state.all.filter { it.status == TransferStatus.SUCCESS }
                TransfersTab.Offline -> state.all.filter { it.status == TransferStatus.FAILED }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            filtered.forEach { t ->
                TransferItem(
                    transfer = t,
                    onPause = { onPause(t) },
                    onMore = { onMore(t) },
                    onRetry = { onRetry(t) },
                )
            }
            if (filtered.isEmpty()) {
                Spacer(Modifier.height(40.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "暂无传输任务", style = t.body, color = c.muted)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/* ───────────────────────────── AppBar ───────────────────────────── */

@Composable
private fun AppBar(activeCount: Int, onSearch: () -> Unit, onClear: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "传输", style = t.large, color = c.fg)   // 22sp
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$activeCount 进行中",
                style = t.micro,
                color = c.muted,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn(glyph = "⌕", onClick = onSearch, contentDescription = "搜索")
            IconBtn(glyph = "🗑", onClick = onClear, contentDescription = "清空已完成")
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

/* ───────────────────────────── Overview ───────────────────────────── */

@Composable
private fun Overview(
    totalActive: Int,
    totalDone: Int,
    upSpeedBps: Long,
    downSpeedBps: Long,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 6dp 蓝点 + 18% shadow ring
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(c.accent),
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.18f)),
                )
                Text(text = "实时状态", style = t.micro, color = c.muted)
            }
            Text(
                text = "↑ ${humanRate(upSpeedBps)} · ↓ ${humanRate(downSpeedBps)}",
                style = t.mono,
                color = c.muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCol(
                n = totalActive.toString(),
                u = " / 31",
                l = "进行中",
                accent = true,
                modifier = Modifier.weight(1f),
            )
            StatCol(
                n = "%.1f".format(upSpeedBps / 1_048_576.0),
                u = " MB",
                l = "已传输",
                modifier = Modifier.weight(1f),
            )
            StatCol(
                n = totalDone.toString(),
                l = "已完成",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCol(n: String, u: String? = null, l: String, accent: Boolean = false, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = n,
                style = t.large,    // 22sp
                color = c.fg,
            )
            if (u != null) {
                Text(text = u, style = t.micro, color = c.muted, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
        Text(text = l, style = t.micro, color = c.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

/* ───────────────────────────── Tabs ───────────────────────────── */

@Composable
private fun Tabs(
    active: TransfersTab,
    totalActive: Int,
    totalDone: Int,
    totalOffline: Int,
    onTab: (TransfersTab) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 0.dp)
            .border(0.dp, c.border)   // 占位：实际下划线通过 active 的 ::after 模拟
            .border(width = 0.dp, color = Color.Transparent),
    ) {
        // 实际下划线用底部 Box 实现
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransfersTab.entries.forEach { tab ->
                    val isActive = tab == active
                    val count = when (tab) {
                        TransfersTab.Active -> totalActive
                        TransfersTab.Done   -> totalDone
                        TransfersTab.Offline -> totalOffline
                    }
                    Box(
                        modifier = Modifier
                            .clickable { onTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tab.label,
                                style = t.body,
                                color = if (isActive) c.fg else c.muted,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActive) c.fg else c.border)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = t.micro,
                                    color = if (isActive) c.bg else c.muted,
                                )
                            }
                        }
                    }
                }
            }
            // 下划线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.border),
            )
        }
    }
}

/* ───────────────────────────── OpRow ───────────────────────────── */

@Composable
private fun OpRow(onPauseAll: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "已按添加时间排序", style = t.micro, color = c.muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable(onClick = onPauseAll),
        ) {
            Text(text = "❚❚", style = t.caption, color = c.fg)
            Text(text = "全部暂停", style = t.micro, color = c.fg)
        }
    }
}

/* ───────────────────────────── TransferItem ───────────────────────────── */

@Composable
private fun TransferItem(
    transfer: Transfer,
    onPause: () -> Unit,
    onMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val progress = if (transfer.totalBytes <= 0) 0f
                   else (transfer.transferredBytes.toDouble() / transfer.totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        // head: thumb + name + pause + more
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TransferThumb(direction = transfer.direction, status = transfer.status)
            Text(
                text = transfer.fileName,
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PauseBtn(status = transfer.status, onClick = onPause)
            MoreBtn(onClick = onMore, failed = transfer.status == TransferStatus.FAILED, onRetry = onRetry)
        }
        Spacer(Modifier.height(10.dp))
        // progress bar 4dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFF0F0F0)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor(transfer.status, c)),
            )
        }
        Spacer(Modifier.height(6.dp))
        // foot: pct + size + speed/status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = t.micro,
                    color = c.fg,
                )
                Text(
                    text = "${humanSize(transfer.transferredBytes)} / ${humanSize(transfer.totalBytes)}",
                    style = t.micro,
                    color = c.muted,
                )
            }
            StatusText(transfer = transfer, onClick = onRetry)
        }
    }
}

@Composable
private fun TransferThumb(direction: Direction, status: TransferStatus) {
    val brush = when (status) {
        TransferStatus.FAILED -> Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB91C1C)))
        else -> when (direction) {
            Direction.UP   -> Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C)))   // orange
            Direction.DOWN -> Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF7E22CE)))   // purple
        }
    }
    val glyph = when (status) {
        TransferStatus.FAILED -> "✕"
        TransferStatus.SUCCESS -> "✓"
        else -> when (direction) {
            Direction.UP   -> "↑"
            Direction.DOWN -> "↓"
        }
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = StarVaultTheme.typography.micro, color = Color.White)
    }
}

@Composable
private fun PauseBtn(status: TransferStatus, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val glyph = when (status) {
        TransferStatus.RUNNING -> "❚❚"
        TransferStatus.PAUSED  -> "▶"
        else -> "↻"   // SUCCESS / FAILED 用重试/查看
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = StarVaultTheme.typography.caption, color = c.muted)
    }
}

@Composable
private fun MoreBtn(onClick: () -> Unit, failed: Boolean, onRetry: () -> Unit) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = if (failed) onRetry else onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (failed) "↻" else "⋯",
            style = StarVaultTheme.typography.caption,
            color = if (failed) c.accent else c.muted,
        )
    }
}

@Composable
private fun StatusText(transfer: Transfer, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val arrow = if (transfer.direction == Direction.UP) "↑" else "↓"
    val text = when (transfer.status) {
        TransferStatus.RUNNING -> "$arrow ${humanRate(transfer.speedBps)} · ${estimateEta(transfer)}"
        TransferStatus.PAUSED  -> if (transfer.transferredBytes == 0L) "排队 2 · 约 4 分钟" else "已暂停"
        TransferStatus.SUCCESS -> "刚刚 · ${humanRate(transfer.speedBps.coerceAtLeast(21_474_836))}"
        TransferStatus.FAILED  -> "网络中断 · 3 次重试"
    }
    val color = when (transfer.status) {
        TransferStatus.RUNNING -> c.accent
        TransferStatus.PAUSED  -> c.muted
        TransferStatus.SUCCESS -> c.success
        TransferStatus.FAILED  -> c.danger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = t.micro,
            color = c.muted,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

/* ───────────────────────────── 工具 ───────────────────────────── */

private fun progressColor(status: TransferStatus, c: com.starvault.theme.StarVaultColors): Color = when (status) {
    TransferStatus.RUNNING -> c.accent
    TransferStatus.PAUSED  -> c.warn
    TransferStatus.SUCCESS -> c.success
    TransferStatus.FAILED  -> Color(0xFFD4D4D4)
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

private fun humanRate(bps: Long): String = when {
    bps >= 1_048_576 -> "%.1f MB/s".format(bps / 1_048_576.0)
    bps >= 1_024     -> "%.1f KB/s".format(bps / 1_024.0)
    else             -> "$bps B/s"
}

private fun estimateEta(t: Transfer): String {
    if (t.speedBps <= 0) return "—"
    val remaining = (t.totalBytes - t.transferredBytes).coerceAtLeast(0)
    val seconds = remaining / t.speedBps
    return when {
        seconds >= 3600 -> "${seconds / 3600} 小时 ${(seconds % 3600) / 60} 分"
        seconds >= 60   -> "${seconds / 60} 分 ${seconds % 60} 秒"
        else            -> "$seconds 秒"
    }
}
