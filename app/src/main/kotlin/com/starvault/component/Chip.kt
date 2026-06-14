package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 通用小 chip：可点 + accent-soft 底 + fg 文字 + 3dp 圆角。
 * 用作：tag 标识、tab 选中态、segmented 按钮组单元。
 */
@Composable
fun Chip(
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val bg: Color = if (selected) c.accentSoft else c.surface
    val fg: Color = if (selected) c.accent else c.muted
    Box(
        modifier = modifier
            .clip(StarVaultTheme.shapes.xs)
            .background(bg)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = t.caption, color = fg)
    }
}
