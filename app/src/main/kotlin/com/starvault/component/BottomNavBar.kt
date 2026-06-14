package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.starvault.nav.Route
import com.starvault.theme.StarVaultTheme

/**
 * 4-tab 全局底栏。
 *  - 高度 64dp + 底部 46dp 设备区留白（navigationBarsPadding）
 *  - 选中态：accent 色图标 + 标题；未选：muted 色
 *  - 通过 hasRoute<T>() 判定，不依赖字符串
 *  - 图标用 unicode 字形（与 HomeScreen AppBar 同模式）—— 避开了 ImageVector path DSL 的
 *    老坑，等 Valkyrie 图形上线后用真 SVG 替换。
 */
private data class TabSpec(
    val label: String,
    val icon: String,                          // unicode glyph 占位
    val route: Route,
    val isSelected: (NavDestination) -> Boolean,
)

@Composable
fun BottomNavBar(
    nav: NavHostController,
    destination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    // icon 字形选择：
    //   ⌂  U+2302  house               → Home
    //   ⌹  U+2339  quad vertical bars  → Files（4 条像文档行）
    //   ⧉  U+29C9  two joined squares  → Album（像相册网格）
    //   ◉  U+25C9  fish-eye            → Profile（头像圆形）
    val tabs = listOf(
        TabSpec("首页", "⌂", Route.Home)    { it.hasRoute<Route.Home>() },
        TabSpec("文件", "⌹", Route.Files()) { it.hasRoute<Route.Files>() },
        TabSpec("相册", "⧉", Route.Album)   { it.hasRoute<Route.Album>() },
        TabSpec("我的", "◉", Route.Profile) { it.hasRoute<Route.Profile>() },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface),
    ) {
        // top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StarVaultTheme.dimens.borderHairline)
                .background(c.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(StarVaultTheme.dimens.bottomBarHeight)
                .navigationBarsPadding()
                .padding(horizontal = 4.dp),
        ) {
            tabs.forEach { tab ->
                val selected = destination?.let(tab.isSelected) ?: false
                val tint: Color = if (selected) c.accent else c.muted
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            nav.navigate(tab.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Route.Home) { saveState = true }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.icon,
                            style = t.subtitle,        // 16sp glyph
                            color = tint,
                            modifier = Modifier.size(22.dp),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = tab.label,
                            style = t.micro,
                            color = tint,
                        )
                    }
                }
            }
        }
    }
}
