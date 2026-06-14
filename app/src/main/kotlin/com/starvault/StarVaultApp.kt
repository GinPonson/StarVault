package com.starvault

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.starvault.component.BottomNavBar
import com.starvault.nav.Route
import com.starvault.nav.StarVaultNavHost
import com.starvault.theme.StarVaultTheme

/**
 * App 根容器（在 [MainActivity] 中被 [StarVaultTheme] 包裹）。
 *
 *  - 单一 [NavHostController] 由顶层 [rememberNavController] 持有，保证整个 App 生命期共享同一栈
 *  - 通过 [currentBackStackEntryAsState] 订阅栈顶变化，重组本 Composable 时 [destination] 自动刷新
 *  - 仅当栈顶属于 4 个 Tab 之一（Home/Files/Transfers/Profile）时才显示 [BottomNavBar]，
 *    Login/Player/Share/Wallpaper 等全屏页隐藏底栏（与 design HTML mockup 一致）
 *  - 用 [Route] sealed 类型 + `hasRoute<T>()` 判定，避免比对字符串导致的拼写错误
 */
@Composable
fun StarVaultApp() {
    // -- 全局导航控制器（仅创建一次）--
    val nav = rememberNavController()
    // -- 订阅当前栈顶 entry，destination 用来匹配是否在底栏所辖的 4 个 tab --
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    // -- 判定是否展示 BottomNavBar --
    //   只在 4 个 tab 顶层展示；首次启动时 backStack == null，destination == null，因此默认隐藏
    val showBottomBar = destination?.let { d ->
        d.hasRoute<Route.Home>()      ||
        d.hasRoute<Route.Files>()     ||
        d.hasRoute<Route.Transfers>() ||
        d.hasRoute<Route.Profile>()
    } ?: false

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(nav = nav, destination = destination) },
        // Scaffold 默认 surface；指定 design tokens 的 bg 与全屏底色一致（#FAFAFA）
        containerColor = StarVaultTheme.colors.bg,
    ) { padding ->
        // 内容区使用 Scaffold 计算出的 inset padding，避免被底栏遮挡
        StarVaultNavHost(
            navController = nav,
            modifier = Modifier.padding(padding),
        )
    }
}
