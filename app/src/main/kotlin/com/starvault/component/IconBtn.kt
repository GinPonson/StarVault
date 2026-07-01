package com.starvault.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 40dp 圆形 icon 按钮 — 5 个 tab root 屏 AppBar 复用。
 *
 *  - box       40dp, CircleShape, clickable
 *  - icon      20dp, tint 默认 [StarVaultTheme.colors.fg](也支持外部传入白色用于深色背景)
 *  - 用于 [ScreenAppBar] 的 actions slot
 *
 * 不在范围:
 *  - [PlayerTopBar] 内的 40dp icon 按钮:tint 永远 Color.White,沿用屏内私有 [PlayerIconBtn]
 */
@Composable
fun IconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    tint: Color = StarVaultTheme.colors.fg,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
