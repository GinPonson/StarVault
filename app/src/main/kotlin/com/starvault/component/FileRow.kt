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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

/**
 * Files / Album 屏的列表行单元。
 *  - 左侧 36dp 圆形 icon 缩略（不同 FileType 配 accent-soft / accent 不同色）
 *  - 中间 文件名 + meta（大小 / mtime / tag）
 *  - 整体 clickable
 */
@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val thumbColor: Color = when (file.type) {
        FileType.FOLDER -> c.accentSoft
        FileType.VIDEO  -> c.accentSoft
        FileType.IMAGE  -> c.accentSoft
        FileType.DOC    -> c.border
        FileType.AUDIO  -> c.accentSoft
        FileType.OTHER  -> c.border
    }
    val tagColor: Color? = when (file.tag) {
        TagColor.TAG1 -> c.tag1
        TagColor.TAG2 -> c.tag2
        TagColor.TAG3 -> c.tag3
        TagColor.TAG4 -> c.tag4
        TagColor.TAG5 -> c.tag5
        null -> null
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(thumbColor),
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
            Spacer(Modifier.size(2.dp))
            Text(
                text = formatMeta(file),
                style = t.caption,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (tagColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tagColor),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = file.type.name,
            style = t.micro,
            color = c.muted,
        )
    }
}

private fun formatMeta(file: FileItem): String {
    val size = file.sizeBytes?.let { humanSize(it) } ?: "—"
    val date = formatDate(file.mtime)
    return "$size · $date"
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

private fun formatDate(ms: Long): String {
    // 简单格式化：YYYY-MM-DD（避免 java.time 复杂依赖）
    val totalSec = ms / 1000
    val year = 1970 + (totalSec / (365 * 24 * 3600)).toInt()
    val month = ((totalSec / (30 * 24 * 3600)) % 12 + 1).toInt().coerceIn(1, 12)
    val day = ((totalSec / (24 * 3600)) % 28 + 1).toInt().coerceIn(1, 28)
    return "%04d-%02d-%02d".format(year, month, day)
}
