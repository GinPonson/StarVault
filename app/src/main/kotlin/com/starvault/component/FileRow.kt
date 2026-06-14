package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

/**
 * Home / Files / Album 屏的共享文件行（对应 design HTML `.file-row`）。
 *
 *  ┌─[40dp gradient thumb]─[12dp]─[name + meta]─[8dp]─[more icon]
 *
 *  - thumb        40dp 圆角 9dp + per-type 渐变背景 + 内置类型 icon
 *  - meta         "<size> · <duration|resolution|count> · [tag chip]"
 *  - tag chip     16dp 高 / 4dp 圆角 / accent-soft 风格底 + 5 色前景
 *  - more         18dp 三点 icon (悬浮态，可点)，回调走 onMore
 *
 * @param file        要渲染的 FileItem
 * @param onClick     行点击（默认进入文件夹 / 打开播放器）
 * @param onMore      ⋯ 点击（弹出菜单），可为空时不渲染 more icon
 * @param metaSecondaryOverride  强制覆写 meta 第二段（size 之后的 · 段）。
 *                                Home 屏传相对时间（"2 小时前"），其它屏不传时
 *                                自动用 [file].durationOrCount 或 formatDate(mtime)。
 */
@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    metaSecondaryOverride: String? = null,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileThumb(type = file.type)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = t.body,
                color = c.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(3.dp))
            FileMeta(file = file, secondaryOverride = metaSecondaryOverride)
        }
        if (onMore != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⋯", style = t.body, color = c.muted)
            }
        }
    }
}

/**
 * 40dp gradient thumbnail。颜色与 design HTML `.file-thumb.{folder|video|image|...}` 一一对应。
 * 中央渲染一行类型缩写（"F"/"V"/"I"/"A"/"D"/"Z"）作为 stub icon，
 * 等 Valkyrie 上线后用真 SVG 替换。
 */
@Composable
private fun FileThumb(type: FileType, modifier: Modifier = Modifier) {
    val (brush, letter) = thumbBrushAndLetter(type)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = StarVaultTheme.typography.micro,
            color = Color.White,
        )
    }
}

/** 根据文件类型映射 design HTML 中对应的 135° 渐变与 stub 字母。 */
private fun thumbBrushAndLetter(type: FileType): Pair<Brush, String> = when (type) {
    FileType.FOLDER -> Brush.linearGradient(listOf(Color(0xFF6B7280), Color(0xFF4B5563))) to "F"
    FileType.VIDEO  -> Brush.linearGradient(listOf(Color(0xFF2F6FEB), Color(0xFF1D4ED8))) to "V"
    FileType.IMAGE  -> Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C))) to "I"
    FileType.AUDIO  -> Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF7E22CE))) to "A"
    FileType.DOC    -> Brush.linearGradient(listOf(Color(0xFF16A34A), Color(0xFF15803D))) to "D"
    FileType.ZIP    -> Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFFBE185D))) to "Z"
    FileType.OTHER  -> Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280))) to "?"
}

/**
 * meta 行：size · count|duration|relativeTime · tag。各段之间用 "·" 分隔。
 *
 * 解析优先级：
 *  1. [secondaryOverride] 非空 → 强制用它（Home 屏传 "2 小时前" / "今天 09:12"）
 *  2. file.durationOrCount 非空 → 用它（folder: "28 项" / video: "1:42:08" / image: "4032 × 3024"）
 *  3. 文件有 size 但无 durationOrCount → 用 formatDate(mtime)（doc/zip 默认行为）
 */
@Composable
private fun FileMeta(file: FileItem, secondaryOverride: String? = null) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        val firstSegment = when {
            file.sizeBytes != null -> humanSize(file.sizeBytes)
            file.durationOrCount != null -> file.durationOrCount   // folder: "28 项"
            else -> "—"
        }
        Text(text = firstSegment, style = t.caption, color = c.muted)
        val secondSegment = secondaryOverride
            ?: file.durationOrCount
            ?: if (file.sizeBytes != null) formatDate(file.mtime) else null
        if (secondSegment != null) {
            DotSep()
            Text(text = secondSegment, style = t.caption, color = c.muted)
        }
        val tag = file.tag
        if (tag != null) {
            Spacer(Modifier.width(6.dp))
            TagPill(tag = tag)
        }
    }
}

@Composable
private fun DotSep() {
    Text(
        text = " · ",
        style = StarVaultTheme.typography.caption,
        color = StarVaultTheme.colors.muted,
    )
}

/**
 * 文件 meta 中的 tag pill（accent-soft 风格底 + 同色前景），高 16dp。
 */
@Composable
private fun TagPill(tag: FileTag) {
    val (bg, fg) = when (tag.color) {
        TagColor.TAG1 -> Color(0x142F6FEB) to Color(0xFF2F6FEB)
        TagColor.TAG2 -> Color(0x1A9333EA) to Color(0xFF9333EA)
        TagColor.TAG3 -> Color(0x1AEA580C) to Color(0xFFEA580C)
        TagColor.TAG4 -> Color(0x1A16A34A) to Color(0xFF16A34A)
        TagColor.TAG5 -> Color(0x1ADB2777) to Color(0xFFDB2777)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = tag.label,
            style = StarVaultTheme.typography.micro,
            color = fg,
        )
    }
}

/* ───────────────────────────── 工具函数 ───────────────────────────── */

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

/** 简单格式化：YYYY-MM-DD（避免引入 java.time）。 */
private fun formatDate(ms: Long): String {
    val totalSec = ms / 1000
    val year = 1970 + (totalSec / (365 * 24 * 3600)).toInt()
    val month = ((totalSec / (30 * 24 * 3600)) % 12 + 1).toInt().coerceIn(1, 12)
    val day = ((totalSec / (24 * 3600)) % 28 + 1).toInt().coerceIn(1, 28)
    return "%04d-%02d-%02d".format(year, month, day)
}
