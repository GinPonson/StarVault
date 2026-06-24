package com.starvault.ui.share

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starvault.component.Icons
import com.starvault.data.model.FileItem
import com.starvault.theme.StarVaultTheme

/**
 * Share 屏（对应 design/03-share.html 的"链接分享"tab，1:1 复刻）。
 *
 * 整体结构（自上而下）：
 *   ┌─ Background       8 行 file-row 模糊 stub + 32% 黑 scrim
 *   └─ BottomSheet      从底部 22dp 圆角升起：
 *        ├─ Grabber
 *        ├─ SheetHead    "分享与转存" 18sp + 关闭 X
 *        ├─ FileCard     40dp 蓝渐变 thumb + name/meta + "已选 1" 蓝 chip
 *        ├─ TabBar       3 段（链接分享 active / 转存到我的 / 发送给…）
 *        ├─ AccessSelect "谁能访问" + select 行
 *        ├─ CodeRow      提取码(fg 底) + 有效期(1:1)
 *        ├─ ToggleRow ×3 禁止转存 / 仅 VIP 可见 / 下载需登录
 *        ├─ QrRow        88dp QR + 标题/sub/3 个 chip
 *        ├─ ResultRow    链接 + 复制 + 剩余 7 天 + 已复制 3 次
 *        └─ Cta          "立即转存到我的网盘"
 *
 * 状态映射（与 [ShareUiState] 对应）：
 *   - Loading : 仅渲染 sheet 与文件卡占位，其它字段显示 stub
 *   - Ready   : 完整渲染
 *   - Error   : 顶部显示 message
 *
 * @param state            UiState
 * @param onClose          关闭（X）→ popBackStack
 * @param onTab            切换 tab
 * @param onAccessType     切换"谁能访问"选项
 * @param onRegenCode      重新生成提取码
 * @param onExpires        切换有效期
 * @param onForbidTransfer 禁止转存开关
 * @param onVipOnly        仅 VIP 可见
 * @param onLoginRequired  下载需登录
 * @param onCopy           复制链接
 * @param onCta            立即转存 CTA
 */
@Composable
fun ShareScreen(
    state: ShareUiState,
    onClose: () -> Unit,
    onTab: (ShareTab) -> Unit,
    onAccessType: () -> Unit,
    onRegenCode: () -> Unit,
    onExpires: () -> Unit,
    onForbidTransfer: () -> Unit,
    onVipOnly: () -> Unit,
    onLoginRequired: () -> Unit,
    onCopy: () -> Unit,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    Box(modifier = modifier.fillMaxSize().background(c.bg)) {
        // 1) 背景：8 行 file-row stub（模糊用 brightness + alpha 模拟）
        BlurredFileRowsBg()
        // 32% 黑 scrim 覆盖
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f)),
        )

        // 2) 底部 sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(c.surface)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // grabber
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.border),
            )
            Spacer(Modifier.height(12.dp))

            ShareScreenHeader(onClose = onClose)

            // file card
            FileCard(file = state.file)
            Spacer(Modifier.height(16.dp))

            // tab bar
            TabBar(active = state.activeTab, onTab = onTab)
            Spacer(Modifier.height(16.dp))

            // 三个 tab 内容
            val ready = state as? ShareUiState.Ready
            ShareScreenContent(
                state = state,
                ready = ready,
                onAccessType = onAccessType,
                onRegenCode = onRegenCode,
                onExpires = onExpires,
                onForbidTransfer = onForbidTransfer,
                onVipOnly = onVipOnly,
                onLoginRequired = onLoginRequired,
                onCopy = onCopy,
            )

            Spacer(Modifier.height(8.dp))

            ShareScreenFooter(onCta = onCta)
        }
    }
}

/**
 * Share 屏 sheet 顶部 header："分享与转存" 标题 + 右侧 X 关闭按钮。
 */
@Composable
private fun ShareScreenHeader(onClose: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "分享与转存", style = t.title, color = c.fg)  // 20sp
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Close,
                contentDescription = "关闭",
                tint = c.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * Share 屏 sheet 中部内容：根据 activeTab 渲染 Link / Save / Send 三 tab 的内容。
 */
@Composable
private fun ShareScreenContent(
    state: ShareUiState,
    ready: ShareUiState.Ready?,
    onAccessType: () -> Unit,
    onRegenCode: () -> Unit,
    onExpires: () -> Unit,
    onForbidTransfer: () -> Unit,
    onVipOnly: () -> Unit,
    onLoginRequired: () -> Unit,
    onCopy: () -> Unit,
) {
    when (state.activeTab) {
        ShareTab.Link -> LinkTabContent(
            state = ready,
            onAccessType = onAccessType,
            onRegenCode = onRegenCode,
            onExpires = onExpires,
            onForbidTransfer = onForbidTransfer,
            onVipOnly = onVipOnly,
            onLoginRequired = onLoginRequired,
            onCopy = onCopy,
        )
        ShareTab.Save -> SaveTabPlaceholder()
        ShareTab.Send -> SendTabPlaceholder()
    }
}

/**
 * Share 屏 sheet 底部 CTA："立即转存到我的网盘"。
 */
@Composable
private fun ShareScreenFooter(onCta: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.accent)
            .clickable(onClick = onCta),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "立即转存到我的网盘", style = t.subtitle, color = c.accentOn)
    }
}

/* ───────────────────────────── 背景 ───────────────────────────── */

/**
 * 8 行 44dp 圆角 8dp 灰条 (rgba(0,0,0,0.04))，模拟模糊后的 file-row。
 *
 * Phase 1 直接画 8 个 Box：模糊感由外层 32% scrim + 整体 layout 营造，
 * 真实接入可换成 RenderEffect.createBlurEffect(2.dp, 2.dp, Shader.TileMode.CLAMP)
 * （API 31+），但 Paparazzi 不支持 RenderEffect，先用静态近似。
 */
@Composable
private fun BlurredFileRowsBg() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.04f)),
            )
        }
    }
}

/* ───────────────────────────── File Card ───────────────────────────── */

@Composable
private fun FileCard(file: FileItem?) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // thumb: video 蓝渐变
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2F6FEB), Color(0xFF1D4ED8)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Play,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file?.name ?: "—",
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (file?.sizeBytes != null) "${humanSize(file.sizeBytes)} · /影视/科幻/" else "—",
                style = t.micro,
                color = c.muted,
            )
        }
        // 已选 1 蓝 chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(c.accent)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(text = "已选 1", style = t.micro, color = c.accentOn)
        }
    }
}

/* ───────────────────────────── Tab Bar ───────────────────────────── */

@Composable
private fun TabBar(active: ShareTab, onTab: (ShareTab) -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bg)
            .padding(3.dp),
    ) {
        ShareTab.entries.forEach { tab ->
            val isActive = tab == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) c.surface else Color.Transparent)
                    .clickable { onTab(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = t.body,
                    color = if (isActive) c.fg else c.muted,
                )
            }
        }
    }
}

/* ───────────────────────────── Link Tab Content ───────────────────────────── */

@Composable
private fun LinkTabContent(
    state: ShareUiState.Ready?,
    onAccessType: () -> Unit,
    onRegenCode: () -> Unit,
    onExpires: () -> Unit,
    onForbidTransfer: () -> Unit,
    onVipOnly: () -> Unit,
    onLoginRequired: () -> Unit,
    onCopy: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val s = state
    // 1) 谁能访问
    Field(label = "谁能访问") {
        Select(value = s?.accessType ?: "—", onClick = onAccessType)
    }
    // 2) 提取码 + 有效期
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            Field(label = "提取码") {
                // fg 底白字
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.fg)
                        .clickable(onClick = onRegenCode)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = s?.accessCode ?: "—",
                            style = t.mono.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.15f, androidx.compose.ui.unit.TextUnitType.Em)),
                            color = c.accentOn,
                        )
                        Text(text = "↻", style = t.body, color = c.accentOn)
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            Field(label = "有效期") {
                Select(value = "${s?.expiresInDays ?: "—"} 天", onClick = onExpires)
            }
        }
    }
    // 3) 3 个 toggle
    ToggleRow(label = "禁止转存",    on = s?.forbidTransfer ?: false, onToggle = onForbidTransfer)
    Divider()
    ToggleRow(label = "仅 VIP 可见",  on = s?.vipOnly ?: false,        onToggle = onVipOnly)
    Divider()
    ToggleRow(label = "下载需登录",   on = s?.loginRequired ?: false, onToggle = onLoginRequired)
    Spacer(Modifier.height(14.dp))

    // 4) QR + info
    QrRow(
        daysLeft = s?.expiresInDays ?: 0,
        vipOnly = s?.vipOnly ?: false,
        hasCode = s != null,
    )
    Spacer(Modifier.height(14.dp))

    // 5) result
    ResultRow(
        url = s?.link?.url ?: "—",
        daysLeft = s?.expiresInDays ?: 0,
        copiedCount = s?.copiedCount ?: 0,
        onCopy = onCopy,
    )
}

@Composable
private fun SaveTabPlaceholder() {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "转存到我的网盘（Phase 1 占位）", style = t.body, color = c.muted)
    }
}

@Composable
private fun SendTabPlaceholder() {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "发送给好友（Phase 1 占位）", style = t.body, color = c.muted)
    }
}

/* ───────────────────────────── Field / Select / Toggle ───────────────────────────── */

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    val t = StarVaultTheme.typography
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            text = label,
            style = t.micro,
            color = StarVaultTheme.colors.muted,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun Select(value: String, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = value, style = t.body, color = c.fg)
        Text(text = "▾", style = t.caption, color = c.muted)
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = t.body, color = c.fg)
        Switch(checked = on)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val c = StarVaultTheme.colors
    val bg = if (checked) c.accent else c.border
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StarVaultTheme.dimens.borderHairline)
            .background(StarVaultTheme.colors.border),
    )
}

/* ───────────────────────────── QR Row ───────────────────────────── */

@Composable
private fun QrRow(daysLeft: Int, vipOnly: Boolean, hasCode: Boolean) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QrStub(modifier = Modifier.size(88.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "扫码分享 / 转存", style = t.body, color = c.fg)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "朋友扫码后可直接转存到自己的网盘，无需登录提取码。",
                style = t.micro,
                color = c.muted,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasCode) AccentChip("$daysLeft 天")
                AccentChip("有提取码")
                if (vipOnly) AccentChip("仅 VIP")
            }
        }
    }
}

/** 88dp 简版 QR 占位（conic-gradient 棋盘格，与 design HTML `.qr` 一致）。*/
@Composable
private fun QrStub(modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, c.border, RoundedCornerShape(8.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val cell = size.width / 8f
            // 棋盘格
            for (r in 0 until 8) {
                for (col in 0 until 8) {
                    val color = if ((r + col) % 2 == 0) Color(0xFF111111) else Color.White
                    drawRect(
                        color = color,
                        topLeft = Offset(col * cell, r * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
        // 3 个 finder 框（左上/右上/左下）
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val s = size.width / 3
            val box = s * 0.32f
            // 左上
            drawRect(Color(0xFF111111), topLeft = Offset(2f, 2f), size = Size(s, s))
            drawRect(Color.White,      topLeft = Offset(s * 0.25f, s * 0.25f), size = Size(s * 0.5f, s * 0.5f))
            // 右上
            drawRect(Color(0xFF111111), topLeft = Offset(size.width - s - 2f, 2f), size = Size(s, s))
            drawRect(Color.White,      topLeft = Offset(size.width - s * 0.75f, s * 0.25f), size = Size(s * 0.5f, s * 0.5f))
            // 左下
            drawRect(Color(0xFF111111), topLeft = Offset(2f, size.height - s - 2f), size = Size(s, s))
            drawRect(Color.White,      topLeft = Offset(s * 0.25f, size.height - s * 0.75f), size = Size(s * 0.5f, s * 0.5f))
            // suppress unused
            box
        }
    }
}

@Composable
private fun AccentChip(text: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(c.accentSoft)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = text, style = t.micro, color = c.accent)
    }
}

/* ───────────────────────────── Result Row ───────────────────────────── */

@Composable
private fun ResultRow(url: String, daysLeft: Int, copiedCount: Int, onCopy: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = url,
                style = t.mono,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.accentSoft)
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(text = "复制", style = t.micro, color = c.accent)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StarVaultTheme.dimens.borderHairline)
                .background(c.border),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "◴", style = t.caption, color = c.accent)
                Text(text = "剩余 $daysLeft 天", style = t.micro, color = c.muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "≡", style = t.caption, color = c.accent)
                Text(text = "已复制 $copiedCount 次", style = t.micro, color = c.muted)
            }
        }
    }
}

/* ───────────────────────────── 工具 ───────────────────────────── */

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}
