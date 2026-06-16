package com.starvault.ui.login

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 扫码登录屏（对应 design/00-login.html）。
 *
 * 整体结构 (412dp 设备宽，垂直 scroll)：
 *   ┌─ 48dp 黑色 S mark
 *   │ "扫码登录" h1 + sub
 *   ├─ qr-card: 220x220 QR + 4 accent corner + overlay(扫码状态时) + status row + qr-meta
 *   ├─ 3 步指引 (1fr/1fr/1fr)
 *   └─ agree 协议页脚
 *
 * 状态映射 (与 [LoginUiState] 一一对应)：
 *   - Waiting   : 默认 QR，status "等待扫码…"，dot pulse 动画
 *   - Scanned   : QR 上盖 overlay (avatar+nickname+device)，status "已扫码，请在手机上确认"
 *   - LoggedIn  : overlay 切到 check icon + "登录成功"，status 切 success 色
 *   - Error     : status 行显示 message + 红色 dot
 *
 * @param state         当前 UI 状态
 * @param onScanClick   点击 QR 卡片底部"刷新二维码"前面的演示按钮触发
 * @param onRefresh     点击 qr-meta 的"刷新二维码"触发
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onScanClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val s = StarVaultTheme.shapes
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
    ) {
        // ─── mark (48dp 黑底 + "S" 白字) ──────────────────────
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.fg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "S", style = t.subtitle, color = c.accentOn)
        }
        Spacer(Modifier.height(20.dp))

        // ─── 标题 + 副标题 ────────────────────────────────────
        Text(text = "扫码登录", style = t.display, color = c.fg)
        Spacer(Modifier.height(6.dp))
        Text(text = "使用 StarVault 扫一扫即可登录", style = t.caption, color = c.muted)
        Spacer(Modifier.height(24.dp))

        // ─── QR 卡片 ─────────────────────────────────────────
        val cardDim = state is LoginUiState.Scanned || state is LoginUiState.LoggedIn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .border(width = 1.dp, color = c.border, shape = RoundedCornerShape(20.dp))
                .alpha(if (cardDim) 0.5f else 1f)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // QR 220x220
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    QrPlaceholder(modifier = Modifier.fillMaxSize())
                    QrCornerBrackets()
                    // 扫码/登录态浮层
                    if (state is LoginUiState.Scanned || state is LoginUiState.LoggedIn) {
                        QrOverlay(state = state)
                    }
                }
                Spacer(Modifier.height(20.dp))
                StatusRow(state = state)
                Spacer(Modifier.height(10.dp))
                QrMeta(expireSeconds = state.expireSeconds, onRefresh = onRefresh)
            }
        }

        // ─── 3 步指引 ────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepCard(number = "1", text = "打开 StarVault", modifier = Modifier.weight(1f))
            StepCard(number = "2", text = "点击右上角扫一扫", modifier = Modifier.weight(1f))
            StepCard(number = "3", text = "扫描二维码并确认", modifier = Modifier.weight(1f))
        }

        // ─── 协议 ────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "扫码登录即代表您已阅读并同意", style = t.micro, color = c.muted)
            Spacer(Modifier.height(2.dp))
            Text(text = "《StarVault 用户协议》 · 《隐私政策》", style = t.micro, color = c.muted)
        }

        // ─── 演示按钮 (仅 Phase 1)：底部居中触发扫码 ────────
        if (state is LoginUiState.Waiting) {
            Spacer(Modifier.height(20.dp))
            DemoScanButton(onClick = onScanClick)
        }

        Spacer(Modifier.height(40.dp))
    }
}

/* ───────────────────────────── QR 占位画板 ───────────────────────────── */

/**
 * 25x25 模块的伪 QR：3 个 finder 框 + 时序线 + 中心 logo + 数据区伪随机。
 *
 * 与 design/00-login.html 的 genQR() 算法等价但用固定 seed (避免每次重组都洗牌)。
 * T22 Paparazzi 截图会与 HTML 渲染做像素近似比对——若日后接入真 QR 服务，
 * 替换为 ZXing/QRGen 生成的 Bitmap 即可，外部接口不变。
 */
@Composable
private fun QrPlaceholder(modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    Canvas(modifier = modifier.background(Color.White)) {
        val gridSize = 25
        val cell = size.width / gridSize
        val cells = generateQrCells(gridSize)

        // 黑色模块
        for (r in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (cells[r][col]) {
                    drawRect(
                        color = Color(0xFF111111),
                        topLeft = Offset(col * cell, r * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }

        // 中心 5x5 logo 区：白底 + 边框 + "S" 留给上层 overlay 渲染
        val centerStart = ((gridSize / 2) - 2) * cell
        val logoSize = 5 * cell
        drawRect(
            color = Color.White,
            topLeft = Offset(centerStart, centerStart),
            size = Size(logoSize, logoSize),
        )
        drawRect(
            color = Color(0xFF111111),
            topLeft = Offset(centerStart, centerStart),
            size = Size(logoSize, logoSize),
            style = Stroke(width = 1.2f),
        )
    }

    // 中心 "S" 字
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = "S", style = StarVaultTheme.typography.subtitle, color = c.fg)
    }
}

/**
 * 生成 25x25 的 QR 模块表（true=黑）。
 *
 * 算法：
 *  1. 三个 7x7 finder 框（左上、右上、左下）：外框 + 中心 3x3 实心
 *  2. 行 6 / 列 6 的时序图：交替黑白
 *  3. 中心 5x5 logo 区强制留白（在 Canvas 上以白底覆盖）
 *  4. 数据区用线性同余 PRNG（固定 seed=0x5tarV4ult）产生稳定伪随机
 */
private fun generateQrCells(size: Int): Array<BooleanArray> {
    val cells = Array(size) { BooleanArray(size) }

    // finder 7x7：外圈 + 内 3x3
    fun finder(r0: Int, c0: Int) {
        for (r in 0 until 7) for (c in 0 until 7) {
            val outer = r == 0 || r == 6 || c == 0 || c == 6
            val inner = r in 2..4 && c in 2..4
            cells[r0 + r][c0 + c] = outer || inner
        }
    }
    finder(0, 0); finder(0, size - 7); finder(size - 7, 0)

    // 时序图
    for (i in 8 until size - 8) {
        cells[6][i] = (i + 1) % 2 == 0
        cells[i][6] = (i + 1) % 2 == 0
    }

    // 数据区伪随机（固定 seed，保证截图稳定）
    val center = size / 2
    val logoHalf = 2
    var seed = 0x5747D734  // 'S','t','a','r' XOR-ish seed
    for (r in 0 until size) {
        for (c in 0 until size) {
            if (r < 8 && c < 8) continue
            if (r < 8 && c > size - 9) continue
            if (r > size - 9 && c < 8) continue
            if (r in center - logoHalf..center + logoHalf &&
                c in center - logoHalf..center + logoHalf) continue
            if (r == 6 || c == 6) continue
            // LCG: seed = seed*1103515245 + 12345 (mod 2^31)
            seed = ((seed * 1103515245) + 12345) and 0x7FFFFFFF
            cells[r][c] = ((seed shr 16) and 1) == 1
        }
    }
    return cells
}

/**
 * 4 个 accent 色 L 形 corner brackets，叠在 QR 外四角。
 *  - 28dp x 28dp 各 3dp stroke
 *  - 4dp 圆角，与 design HTML 的 `.qr-corner` 视觉一致
 */
@Composable
private fun QrCornerBrackets() {
    val accent = StarVaultTheme.colors.accent
    Box(modifier = Modifier.fillMaxSize()) {
        QrCorner(accent, Modifier.align(Alignment.TopStart), top = true, start = true)
        QrCorner(accent, Modifier.align(Alignment.TopEnd), top = true, start = false)
        QrCorner(accent, Modifier.align(Alignment.BottomStart), top = false, start = true)
        QrCorner(accent, Modifier.align(Alignment.BottomEnd), top = false, start = false)
    }
}

@Composable
private fun QrCorner(color: Color, modifier: Modifier, top: Boolean, start: Boolean) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val stroke = 3.dp.toPx()
        // 横边
        val hX = if (start) 0f else 0f
        val hY = if (top) 0f else w - stroke
        drawRect(color, topLeft = Offset(hX, hY), size = Size(w, stroke))
        // 竖边
        val vX = if (start) 0f else w - stroke
        val vY = if (top) 0f else 0f
        drawRect(color, topLeft = Offset(vX, vY), size = Size(stroke, w))
    }
}

/* ───────────────────────────── QR Overlay ───────────────────────────── */

/**
 * 扫码态/登录成功态的覆盖层。
 *  - Scanned   : 显示 avatar + nickname + device
 *  - LoggedIn  : 显示 check icon + "登录成功"
 *
 * 与 HTML `.qr-overlay` 一致：白底 96% 透明 + blur + 14dp 圆角，居中堆叠。
 */
@Composable
private fun QrOverlay(state: LoginUiState) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            when (state) {
                is LoginUiState.Scanned -> {
                    // 48dp 黑底头像 + 首字母
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(c.fg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.nickname.take(1),
                            style = t.subtitle,
                            color = c.accentOn,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = state.nickname, style = t.body, color = c.fg)
                    Spacer(Modifier.height(2.dp))
                    Text(text = state.deviceName, style = t.micro, color = c.muted)
                }
                is LoginUiState.LoggedIn -> {
                    // 40dp 浅绿圆 + ✓
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(c.successSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = com.starvault.component.Icons.CheckBold,
                            contentDescription = null,
                            tint = c.success,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = "登录成功", style = t.body, color = c.success)
                }
                else -> Unit  // Waiting/Error 不渲染 overlay
            }
        }
    }
}

/* ───────────────────────────── Status Row ───────────────────────────── */

/**
 * QR 下方的状态行：6dp dot + 文案。
 *  - Waiting : muted 灰，dot pulse 动画
 *  - Scanned : muted 灰，文案 "已扫码，请在手机上确认"
 *  - LoggedIn: success 绿，文案 "登录成功，正在跳转…"
 *  - Error   : danger 红，message
 */
@Composable
private fun StatusRow(state: LoginUiState) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val (dotColor, textColor, label) = when (state) {
        is LoginUiState.Waiting  -> Triple(c.muted, c.muted, "等待扫码…")
        is LoginUiState.Scanned  -> Triple(c.muted, c.muted, "已扫码，请在手机上确认")
        is LoginUiState.LoggedIn -> Triple(c.success, c.success, "登录成功，正在跳转…")
        is LoginUiState.Error    -> Triple(c.danger, c.danger, state.message)
    }
    // Waiting 态下 dot 做 1.0 -> 1.4 缩放 pulse 动画；其它态保持 1f
    val transition = rememberInfiniteTransition(label = "login-pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is LoginUiState.Waiting) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "login-pulse-scale",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size((6 * pulseScale).dp.coerceAtLeast(6.dp))
                .clip(RoundedCornerShape(percent = 50))
                .background(dotColor),
        )
        Spacer(Modifier.size(8.dp))
        Text(text = label, style = t.caption, color = textColor)
    }
}

/* ───────────────────────────── qr-meta ───────────────────────────── */

/**
 * 卡片底部 meta 行：左侧 accent 色 "刷新二维码"，右侧 expire 计时。
 */
@Composable
private fun QrMeta(expireSeconds: Int, onRefresh: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable(onClick = onRefresh),
        ) {
            androidx.compose.material3.Icon(
                imageVector = com.starvault.component.Icons.Refresh,
                contentDescription = "刷新二维码",
                tint = c.accent,
                modifier = Modifier.size(12.dp),
            )
            Text(text = "刷新二维码", style = t.caption, color = c.accent)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "二维码 ", style = t.micro, color = c.muted)
            Text(text = formatExpire(expireSeconds), style = t.caption, color = c.fg)
            Text(text = " 后过期", style = t.micro, color = c.muted)
        }
    }
}

private fun formatExpire(seconds: Int): String {
    val m = (seconds / 60).coerceAtLeast(0)
    val s = (seconds % 60).coerceAtLeast(0)
    return "%02d:%02d".format(m, s)
}

/* ───────────────────────────── Step Card ───────────────────────────── */

/**
 * 3 步指引中的一格：上方 22dp 蓝底数字徽章 + 下方说明文字。
 */
@Composable
private fun StepCard(number: String, text: String, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, style = t.micro, color = c.accent)
        }
        Spacer(Modifier.size(8.dp))
        Text(text = text, style = t.micro, color = c.fg)
    }
}

/* ───────────────────────────── Demo Scan Button ───────────────────────────── */

/**
 * Phase 1 演示按钮：底部居中 pill，点击触发模拟扫码。
 * 真实接入扫码服务后此按钮删除。
 */
@Composable
private fun DemoScanButton(onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(c.fg)
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "演示扫码登录", style = t.caption, color = c.accentOn)
        }
    }
}
