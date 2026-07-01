package com.starvault.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 通用 screen AppBar — 5 个 tab root 屏 (Home / Files / Album / Profile / Transfers) 共用。
 *
 *  - 横向 padding    start = 20dp, end = 20dp(对称)
 *  - 纵向 padding    top   =  8dp, bottom = 12dp
 *  - 总高            8 + 40 + 12 = 60dp(natural,跟随 40dp icon 高度)
 *  - 标题字号        t.large 22sp SemiBold
 *  - 右侧 icon 间距  4dp
 *  - 标题与左 back  4dp 间距(仅在 onBack != null 时)
 *  - 标题与 subtitle badge  8dp 间距 + t.micro muted(仅在 subtitle != null 时,Transfers 用)
 *
 * 不在范围:
 *  - Player:黑底 48dp,媒体沉浸上下文,沿用 [PlayerTopBar]
 *  - Wallpaper:fixed 56dp + 1dp border + bg 底色,未统一
 *  - Search:搜索框,非传统 AppBar
 *  - Login / Share:分别居中 logo 和 bottom sheet 顶部,性质不同
 */
@Composable
fun ScreenAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBtn(
                icon = Icons.Back,
                onClick = onBack,
                contentDescription = "返回",
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = t.large, color = c.fg)
            if (subtitle != null) {
                Text(text = subtitle, style = t.micro, color = c.muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            actions()
        }
    }
}
