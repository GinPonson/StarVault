package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 通用顶栏：左侧 Back/More icon + 居中标题 + 右侧可选 action。
 *
 *  - height       52dp
 *  - bottomBorder 1dp accent 12% 透明（与 mockup `.topbar` 边线对齐）
 *  - iconSize     24dp
 */
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(c.bg)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Left: Back icon
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Back,
                    contentDescription = "Back",
                    tint = c.fg,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        // Center: title
        Text(
            text = title,
            style = t.subtitle,
            color = c.fg,
        )
        // Right: More icon
        if (onMore != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.More,
                    contentDescription = "More",
                    tint = c.fg,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
