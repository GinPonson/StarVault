package com.starvault.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.starvault.component.Icons
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

/**
 * Player 屏（对应 design/02-player.html 的视频预览 1:1 复刻）。
 *
 * 整体结构（垂直滚动）：
 *   ┌─ TopBar       返回 + 字幕/投屏/更多
 *   ├─ Canvas       黑色画布 + radial gradient + 中心播放圆 + chips
 *   ├─ Controls     进度条 + 上一集/播放/下一集 + 倍速/下载/全屏
 *   ├─ Info         标题 + sub(size · codec · date) + 3 个 action
 *   ├─ TagRow       4 个 tag pill（影视/4K源/未加密/添加标签）
 *   ├─ MetaGrid     2×2 文件信息（位置/上传者/SHA-1/下载次数）
 *   └─ Related      3 条相关视频（thumbnail + duration + info）
 *
 * 状态映射：
 *   - Loading  : canvas 中央显示 "载入中…"
 *   - Ready    : 完整渲染
 *   - Error    : canvas 中央显示 message
 *
 * @param state      UiState
 * @param onBack     TopBar 返回
 * @param onTogglePlay  中心播放 / 控制条播放 按钮
 * @param onSeek     拖动进度条（0..1）
 * @param onSpeed    倍速切换
 * @param onDownload 下载到本地
 * @param onTransfer 转存
 * @param onShare    分享
 * @param onAddTag   添加标签
 * @param onTagClick 已有 tag 的取消 / 再点
 * @param onRelated  相关推荐点击
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeed: () -> Unit,
    onDownload: () -> Unit,
    onTransfer: () -> Unit,
    onShare: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    onRelated: (RelatedVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        // 1) 顶部工具栏（黑底白字）
        PlayerTopBar(onBack = onBack)

        // 2) 画布
        PlayerCanvas(
            state = state,
            onTogglePlay = onTogglePlay,
        )

        // 3) 控制条（黑底白字）
        PlayerControls(
            state = state,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onSpeed = onSpeed,
        )

        // 4) 文件信息（fg 底）
        InfoSection(
            file = state.file,
            tags = state.tags,
            isAddingTag = state.isAddingTag,
            resolution = (state as? PlayerUiState.Ready)?.resolution ?: "—",
            codec = (state as? PlayerUiState.Ready)?.codec ?: "—",
            savedAt = (state as? PlayerUiState.Ready)?.savedAt ?: "—",
            onDownload = onDownload,
            onTransfer = onTransfer,
            onShare = onShare,
            onAddTag = onAddTag,
            onTagClick = onTagClick,
        )

        // 5) meta grid
        MetaGrid(
            state = state,
        )

        // 6) 相关推荐
        val related = (state as? PlayerUiState.Ready)?.related ?: emptyList()
        RelatedSection(
            related = related,
            onClick = onRelated,
        )

        Spacer(Modifier.height(40.dp))
    }
}

/* ───────────────────────────── Top Bar ───────────────────────────── */

@Composable
private fun PlayerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PlayerIconBtn(icon = Icons.Back,       onClick = onBack, contentDescription = "返回")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PlayerIconBtn(icon = Icons.Subtitle, onClick = {}, contentDescription = "字幕")
            PlayerIconBtn(icon = Icons.Cast,     onClick = {}, contentDescription = "投屏")
            PlayerIconBtn(icon = Icons.More,     onClick = {}, contentDescription = "更多")
        }
    }
}

@Composable
private fun PlayerIconBtn(icon: ImageVector, onClick: () -> Unit, contentDescription: String) {
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
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ───────────────────────────── Canvas ───────────────────────────── */

/**
 * 黑色画布：radial gradient 模拟 HTML `.canvas` 的色彩 + 中心播放圆。
 *
 *  - 2 个 radial gradient（左上蓝 18% / 右下深 40%）叠在 #0A0A0A 底色上
 *  - 左上角 "在线播放 · 已缓冲 X MB" 提示（带蓝点 pulse 动画）
 *  - 右上角 3 个 chip：quality(accent)/speed/audio
 *  - 中心 76dp 白色圆 + 三角（play）/ Loading/Error 时换文本
 */
@Composable
private fun PlayerCanvas(state: PlayerUiState, onTogglePlay: () -> Unit) {
    val ready = state as? PlayerUiState.Ready
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)         // 画布比例与 HTML 视觉一致
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x2E2F6FEB),          // rgba(47,111,235,0.18) ≈ 0x2E
                        Color.Transparent,
                    ),
                    radius = 1200f,
                ),
            )
            .background(Color(0xFF0A0A0A)),
    ) {
        // 二次 radial 右下深色（用绝对定位 Box 实现）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x660F172A),       // rgba(15,23,42,0.40) ≈ 0x66
                            Color.Transparent,
                        ),
                        radius = 1400f,
                    ),
                ),
        )

        // 左上角：在线播放 + 蓝点 pulse
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PulseDot()
            Text(
                text = "在线播放 · 已缓冲 ${ready?.bufferedMb ?: "0.0"} MB",
                style = t.micro,
                color = Color.White,
            )
        }

        // 右上角：3 个 chip
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip(text = ready?.qualityChip ?: "—", accent = true)
            Chip(text = ready?.speedChip ?: "—", accent = false)
            Chip(text = ready?.audioChip ?: "—", accent = false)
        }

        // 中心：根据 state 渲染
        when (state) {
            is PlayerUiState.Ready -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Pause else Icons.Play,
                        contentDescription = if (state.isPlaying) "暂停" else "播放",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            is PlayerUiState.Loading -> {
                Text(
                    text = "载入中…",
                    style = t.body,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is PlayerUiState.Error -> {
                Text(
                    text = state.message,
                    style = t.body,
                    color = c.danger,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** 半透明 chip（"1080P" 蓝底 / 其它 50% 黑底），圆角 6dp。*/
@Composable
private fun Chip(text: String, accent: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (accent) Color(0xD92F6FEB)   // rgba(47,111,235,0.85)
                else Color.Black.copy(alpha = 0.5f),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = StarVaultTheme.typography.micro,
            color = Color.White,
        )
    }
}

/** 6dp 蓝点 + 1.6s pulse box-shadow（HTML `.rec-dot`）。*/
@Composable
private fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val ringAlpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-ring",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((6 + 8 * (1 - ringAlpha / 0.7f)).dp.coerceAtLeast(6.dp))
                .clip(CircleShape)
                .background(Color(0xFF2F6FEB).copy(alpha = ringAlpha)),
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF2F6FEB)),
        )
    }
}

/* ───────────────────────────── Controls ───────────────────────────── */

/**
 * 底部控制条（黑底）：进度条 + 6 个 ctrl-btn。
 *
 *  - 进度条：3dp 灰底 + accent 蓝填充 32% + 12dp 白点 thumb
 *  - 时间文本："32:14 / 1:42:08" tabular-nums
 *  - ctrl 6 键：上一集 / 播放(白底) / 下一集 | 倍速 / 下载 / 全屏
 */
@Composable
private fun PlayerControls(
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeed: () -> Unit,
) {
    val ready = state as? PlayerUiState.Ready
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 进度条
        Progress(
            progress = state.progress,
            position = ready?.position ?: "00:00",
            duration = ready?.duration ?: "00:00",
            onSeek = onSeek,
        )
        Spacer(Modifier.height(8.dp))
        // 6 ctrl btn
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左侧：上一集 / 播放 / 下一集
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CtrlBtn(icon = Icons.Prev, contentDescription = "上一集", onClick = {})
                CtrlBtn(
                    icon = if (state.isPlaying) Icons.Pause else Icons.Play,
                    contentDescription = "播放",
                    onClick = onTogglePlay,
                    isPrimary = true,
                )
                CtrlBtn(icon = Icons.Next, contentDescription = "下一集", onClick = {})
            }
            // 右侧：倍速 / 下载 / 全屏
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CtrlBtn(icon = Icons.Clock,        contentDescription = "倍速", onClick = onSpeed)
                CtrlBtn(icon = Icons.DownloadInto, contentDescription = "下载", onClick = {})
                CtrlBtn(icon = Icons.Fullscreen,   contentDescription = "全屏", onClick = {})
            }
        }
    }
}

@Composable
private fun Progress(progress: Float, position: String, duration: String, onSeek: (Float) -> Unit) {
    val c = StarVaultTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { /* stub: Phase 1 暂不实现精确 seek 拖动 */ },
        ) {
            // filled
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.accent),
            )
            // thumb
            Box(
                modifier = Modifier
                    .padding(start = 0.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.CenterStart)
                    .padding(start = (progress * 100).coerceIn(0f, 100f).dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$position / $duration",
            style = StarVaultTheme.typography.micro,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CtrlBtn(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
) {
    val size = if (isPrimary) 44.dp else 36.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPrimary) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) Color(0xFF111111) else Color.White,
            modifier = Modifier.size(if (isPrimary) 22.dp else 20.dp),
        )
    }
}

/* ───────────────────────────── Info Section ───────────────────────────── */

@Composable
private fun InfoSection(
    file: FileItem?,
    tags: List<FileTag>,
    isAddingTag: Boolean,
    resolution: String,
    codec: String,
    savedAt: String,
    onDownload: () -> Unit,
    onTransfer: () -> Unit,
    onShare: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp),
    ) {
        // title
        Text(
            text = file?.name ?: "—",
            style = t.subtitle,    // 16sp → 接近 HTML 17sp
            color = c.fg,
        )
        Spacer(Modifier.height(6.dp))
        // sub
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = file?.sizeBytes?.let { humanSize(it) } ?: "—",
                style = t.caption,
                color = c.muted,
            )
            SubDot()
            Text(text = "$resolution · $codec", style = t.caption, color = c.muted)
            SubDot()
            Text(text = "$savedAt 存入", style = t.caption, color = c.muted)
        }
        // action row
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn(text = "下载到本地", primary = true, onClick = onDownload, modifier = Modifier.weight(1f))
            ActionBtn(text = "转存", primary = false, onClick = onTransfer, modifier = Modifier.weight(1f))
            ActionBtn(text = "分享", primary = false, onClick = onShare, modifier = Modifier.weight(1f))
        }
        // tag row
        Spacer(Modifier.height(14.dp))
        TagRow(tags = tags, isAddingTag = isAddingTag, onAddTag = onAddTag, onTagClick = onTagClick)
        // divider
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StarVaultTheme.dimens.borderHairline)
                .background(c.border),
        )
    }
}

@Composable
private fun SubDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(StarVaultTheme.colors.muted),
    )
}

@Composable
private fun ActionBtn(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (primary) c.fg else c.surface)
            .border(1.dp, c.border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = t.caption,
            color = if (primary) c.accentOn else c.fg,
        )
    }
}

@Composable
private fun TagRow(
    tags: List<FileTag>,
    isAddingTag: Boolean,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            TagPill(tag = tag, onClick = { onTagClick(tag.label) })
        }
        if (isAddingTag) {
            AddTagPill(onClick = onAddTag)
        }
    }
}

@Composable
private fun TagPill(tag: FileTag, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val swatch = tagSwatchColor(tag.color)
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, c.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(swatch),
        )
        Text(text = tag.label, style = t.micro, color = c.fg)
    }
}

@Composable
private fun AddTagPill(onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, c.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "+", style = t.micro, color = c.muted)
        Text(text = "添加标签", style = t.micro, color = c.muted)
    }
}

/* ───────────────────────────── Meta Grid ───────────────────────────── */

@Composable
private fun MetaGrid(state: PlayerUiState) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val ready = state as? PlayerUiState.Ready
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
    ) {
        Text(
            text = "文件信息",
            style = t.caption.copy(letterSpacing = 0.04.em),
            color = c.muted,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MetaCell(k = "位置", v = ready?.path ?: "—", modifier = Modifier.weight(1f))
                MetaCell(k = "上传者", v = ready?.uploader ?: "—", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MetaCell(k = "SHA-1", v = ready?.sha1 ?: "—", modifier = Modifier.weight(1f), mono = true)
                MetaCell(k = "下载次数", v = (ready?.downloadCount ?: 0).toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetaCell(k: String, v: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier = modifier) {
        Text(text = k, style = t.micro, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Text(
            text = v,
            style = if (mono) t.mono else t.body,
            color = c.fg,
        )
    }
}

/* ───────────────────────────── Related ───────────────────────────── */

@Composable
private fun RelatedSection(related: List<RelatedVideo>, onClick: (RelatedVideo) -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "相关推荐",
                style = t.caption.copy(letterSpacing = 0.04.em),
                color = c.muted,
            )
            Text(
                text = "共 ${related.size} 条",
                style = t.caption,
                color = c.muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        related.forEach { v ->
            RelatedRow(video = v, onClick = { onClick(v) })
        }
    }
}

@Composable
private fun RelatedRow(video: RelatedVideo, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // thumb
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(video.thumbColorHex)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Play,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            // duration 角标
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = video.durationText,
                    style = StarVaultTheme.typography.micro,
                    color = Color.White,
                )
            }
        }
        // info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${video.sizeText} · ${video.dateText}",
                style = t.micro,
                color = c.muted,
            )
        }
    }
}

/* ───────────────────────────── 工具 ───────────────────────────── */

@Composable
private fun tagSwatchColor(color: TagColor): Color = when (color) {
    TagColor.TAG1 -> Color(0xFF2F6FEB)
    TagColor.TAG2 -> Color(0xFF9333EA)
    TagColor.TAG3 -> Color(0xFFEA580C)
    TagColor.TAG4 -> Color(0xFF16A34A)
    TagColor.TAG5 -> Color(0xFFDB2777)
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}
