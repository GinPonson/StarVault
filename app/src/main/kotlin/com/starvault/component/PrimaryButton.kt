package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 主按钮：实心 accent 底 + 圆角 9dp + 白色文字。
 * 与 mockup `.btn-primary` 对齐（design/00-login.html 提交按钮）。
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val bg = if (enabled) c.accent else c.muted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .let { if (enabled) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = t.subtitle,
            color = c.accentOn,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
