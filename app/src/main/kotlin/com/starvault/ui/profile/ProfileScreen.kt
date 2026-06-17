package com.starvault.ui.profile

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme

/**
 * Profile 屏（严格 1:1 对应 design/05-profile.html 的 body 段）。
 *
 * 整体结构（垂直滚动，6 段）：
 *   ┌─ Header       "我的" + 钱包/设置 icon-btn
 *   ├─ StorageCard  132dp 环 + 71% + 5 行 breakdown + 剩余/扩容
 *   ├─ WpCard       壁纸引擎 + off 徽 + 单行 sub
 *   ├─ Section      3 row（我的分享 / 回收站 / 设备管理）
 *   ├─ Section      3 row（隐私 / 外观 / 帮助）
 *   └─ Logout       "退出登录" muted → danger on hover
 *
 * 设计历史：早期版本错误地把头像 UserCard 和 VIP 卡加进来了，但 design body 里没有
 * 那两段（CSS 定义了但未引用）。当前 6 段是 design 真有的。
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onWallet: () -> Unit = {},
    onSettings: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onRow: (RowItem) -> Unit = {},
    onWallpaper: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    when (state) {
        is ProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
                Text("加载中…", style = StarVaultTheme.typography.body, color = c.muted)
            }
        }
        is ProfileUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
                Text(state.message, style = StarVaultTheme.typography.body, color = c.danger)
            }
        }
        is ProfileUiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(c.bg)
                    .verticalScroll(rememberScrollState()),
            ) {
                ProfileHeader(onWallet = onWallet, onSettings = onSettings)
                StorageCard(storage = state.storage, onUpgrade = onUpgrade)
                WallpaperCard(wallpaper = state.wallpaper, onClick = onWallpaper)
                SectionBox(rows = state.commonRows, onRow = onRow)
                SectionBox(rows = state.settingRows, onRow = onRow)
                LogoutRow(onLogout = onLogout)
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

/* ───────────────────── Header ───────────────────── */

@Composable
private fun ProfileHeader(onWallet: () -> Unit, onSettings: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "我的", style = t.large, color = c.fg)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn(icon = Icons.Wallet, onClick = onWallet, contentDescription = "钱包")
            IconBtn(icon = Icons.Settings, onClick = onSettings, contentDescription = "设置")
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit, contentDescription: String) {
    val c = StarVaultTheme.colors
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
            tint = c.fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ───────────────────── (历史 UserCard 已删除，design body 里不存在) ───────────────────── */

/* ───────────────────── StorageCard ───────────────────── */

@Composable
private fun StorageCard(storage: Storage, onUpgrade: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 标题：userName 非空时拼 "云端空间 — Alice"（em dash + 空格）；空时仅 "云端空间"
            val title = if (storage.userName.isNotBlank()) "云端空间 — ${storage.userName}" else "云端空间"
            Text(text = title, style = t.micro, color = c.muted)
            Text(text = storage.releaseDate, style = t.caption, color = c.muted)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StorageRing(usedPct = storage.usedPct, totalLabel = storage.totalLabel)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                storage.breakdowns.forEach { bd ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(bd.swatch),
                        )
                        Text(text = bd.label, style = t.caption, color = c.fg, modifier = Modifier.weight(1f))
                        Text(text = bd.sizeText, style = t.mono, color = c.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = c.border, thickness = 1.dp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "剩余 ", style = t.caption, color = c.muted)
                Text(text = storage.remainingGb, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = c.fg)
                Spacer(Modifier.width(8.dp))
                Text(text = "·", style = t.caption, color = c.muted)
                Spacer(Modifier.width(8.dp))
                Text(text = "回收站 ", style = t.caption, color = c.muted)
                Text(text = storage.trashGb, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = c.fg)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onUpgrade),
            ) {
                Text(text = "扩容", style = t.body, color = c.accent)
                Text(text = "→", style = t.body, color = c.accent)
            }
        }
    }
}

@Composable
private fun StorageRing(usedPct: Int, totalLabel: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val ringSize = 132.dp
    val strokeWidth = 12f
    val ratio = (usedPct.coerceIn(0, 100)) / 100f
    val strokeColor = if (usedPct >= 90) c.warn else c.accent
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val pad = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = Color(0xFFF0F0F0),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = 360f * ratio,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$usedPct%",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = c.fg,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = "已用 / $totalLabel", style = t.micro, color = c.muted)
        }
    }
}

/* ───────────────────── (历史 VipCard 已删除，design body 里不存在) ───────────────────── */

/* ───────────────────── WallpaperCard ───────────────────── */

@Composable
private fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Storage,
                contentDescription = null,
                tint = c.fg,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "壁纸引擎",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = c.fg,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (wallpaper.enabled) c.accent.copy(alpha = 0.10f) else Color(0x14000000))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (wallpaper.enabled) "启用中" else "未启用",
                        style = t.micro,
                        color = if (wallpaper.enabled) c.accent else c.muted,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(text = wallpaper.subText, style = t.caption, color = c.muted)
        }
        Icon(
            imageVector = Icons.ChevronRight,
            contentDescription = null,
            tint = c.muted,
            modifier = Modifier.size(14.dp),
        )
    }
}

/* ───────────────────── SectionBox ───────────────────── */

@Composable
private fun SectionBox(rows: List<RowItem>, onRow: (RowItem) -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp)),
    ) {
        rows.forEachIndexed { idx, r ->
            if (idx > 0) {
                HorizontalDivider(
                    color = c.border,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 62.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRow(r) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (r.iconAccent) c.accent.copy(alpha = 0.10f) else c.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = r.icon,
                        contentDescription = null,
                        tint = if (r.iconAccent) c.accent else c.fg,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = r.label,
                    style = t.body,
                    color = c.fg,
                    modifier = Modifier.weight(1f),
                )
                if (r.rightBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(c.fg)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(text = r.rightBadge, style = t.micro, color = c.surface)
                    }
                } else if (r.rightText != null) {
                    Text(text = r.rightText, style = t.caption, color = c.muted)
                }
                Icon(
                    imageVector = Icons.ChevronRight,
                    contentDescription = null,
                    tint = c.muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/* ───────────────────── Logout ───────────────────── */

@Composable
private fun LogoutRow(onLogout: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 24.dp)
            .clickable(onClick = onLogout),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "退出登录",
            style = t.caption,
            color = c.muted,
        )
    }
}
