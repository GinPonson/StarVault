package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
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
 * @param useDesignFallback  无 thumbnailUrl 时是否走 design 渐变色块。
 *                              **Home 屏 mock 数据走 true**（保留设计稿预览视觉）；
 *                              **真实数据屏（Files）走 false**（统一 loading 灰底）。
 *                              缺省 `true`（保持原有 Home 屏行为）。
 */
@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    metaSecondaryOverride: String? = null,
    useDesignFallback: Boolean = true,
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
        FileThumb(
            type = file.type,
            thumbnailUrl = file.thumbnailUrl,
            useDesignFallback = useDesignFallback,
        )
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
                Icon(
                    imageVector = Icons.More,
                    contentDescription = "更多",
                    tint = c.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * 40dp 缩略图。
 *
 * 渲染策略（两种无 URL 路径，由 [useDesignFallback] 控制）：
 *
 *  - `useDesignFallback = true`（**Home 屏 mock 数据专用**）
 *      无 URL → design 渐变色块 + 白色类型 icon（design HTML mock 预览风格）
 *
 *  - `useDesignFallback = false`（**真实数据屏专用**）
 *      无 URL → `#FAFAFA` 灰底 + `#A1A1AA` 灰色类型 icon（统一 loading 视觉）
 *
 * 共同路径（无论 useDesignFallback）：
 *  - 有 URL + 加载中 → `#FAFAFA` 灰底（loading slot 空，透出外层 Box）
 *  - 有 URL + 加载成功 → 真实缩略图（Crop）
 *  - 有 URL + 加载失败 → `#E4E4E7` 深灰底 + `#52525B` 裂图 icon
 *
 * 三态色阶：
 *   loading:  `#FAFAFA` 灰底（zinc-50）
 *   error:    `#E4E4E7` 深灰底 + `#52525B` icon（zinc-200 / zinc-600）
 *
 * @param useDesignFallback  无 URL 时是否走 design 渐变色块。默认 `true`（保留
 *                          Home 屏 mock 预览视觉）。真实数据屏传 `false`。
 */
@Composable
private fun FileThumb(
    type: FileType,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    useDesignFallback: Boolean = true,
) {
    val (placeholderBrush, icon) = thumbBrushAndIcon(type)
    if (thumbnailUrl.isNullOrBlank()) {
        if (useDesignFallback) {
            // ──── Home 屏 mock 预览：design 渐变 + 白色 icon ────
            Box(
                modifier = modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(placeholderBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            // ──── 真实数据屏：灰底 + 灰 icon（统一 loading 视觉） ────
            Box(
                modifier = modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(PlaceholderGray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PlaceholderIconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        return
    }

    // 有 URL：Coil 三态渲染（loading=浅灰, success=图片, error=深灰+裂图）
    //
    // 关键：把 background 放到**外层 Box**，而非 SubcomposeAsyncImage 的 modifier。
    // 实测发现：SubcomposeAsyncImage 的 modifier.background() 在 loading slot 为空时
    // 不会绘制背景（连外层 Box 也不显示），只有 loading slot 内有 composable 才会触发
    // SubcomposeAsyncImage 内部 Box 的渲染。把背景放在外层 Box 上更稳。
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(PlaceholderGray),     // ← 外层 Box 永远显示 loading 灰底
    ) {
        SubcomposeAsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                // loading: 什么都不渲染，外层 Box 灰底 #FAFAFA 透出
            },
            error = {
                // error: 覆盖一个更深灰底 Box + 中央裂图 icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ErrorBgGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.BrokenImage,
                        contentDescription = "缩略图加载失败",
                        tint = BrokenImageTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            success = { state ->
                // success: 真实图片覆盖（Crop）
                androidx.compose.foundation.Image(
                    painter = state.painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            },
        )
    }
}

/** 加载中 / 无 URL 灰底（中性 stone-50，极浅，几乎不抢戏） */
private val PlaceholderGray = Color(0xFFFAFAFA)

/** 加载中 / 无 URL 的类型 icon 颜色（中性 zinc-400，与底色 #FAFAFA 接近但不消失） */
private val PlaceholderIconTint = Color(0xFFA1A1AA)

/** 加载失败灰底（中性 zinc-200，比 loading 深一档，视觉上明显区分） */
private val ErrorBgGray = Color(0xFFE4E4E7)

/** 裂图 icon 颜色（中性 zinc-600，确保在 zinc-200 底上有足够对比度） */
private val BrokenImageTint = Color(0xFF52525B)

/** 根据文件类型映射 design HTML 中对应的 135° 渐变与 SVG 图标。
 *  渐变方向 135° = 左上 → 右下，与 HTML `linear-gradient(135deg, …)` 一致。*/
private fun thumbBrushAndIcon(type: FileType): Pair<Brush, ImageVector> {
    val brush = when (type) {
        FileType.FOLDER -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF6B7280), 1f to Color(0xFF4B5563)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.VIDEO -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF2F6FEB), 1f to Color(0xFF1D4ED8)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.IMAGE -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFFEA580C), 1f to Color(0xFFC2410C)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.AUDIO -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF9333EA), 1f to Color(0xFF7E22CE)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.DOC -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF16A34A), 1f to Color(0xFF15803D)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.ZIP -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFFDB2777), 1f to Color(0xFFBE185D)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
        FileType.OTHER -> Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFF9CA3AF), 1f to Color(0xFF6B7280)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end   = androidx.compose.ui.geometry.Offset(40f, 40f),
        )
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
    return brush to icon
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
