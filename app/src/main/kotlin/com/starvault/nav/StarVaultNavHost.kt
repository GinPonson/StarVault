package com.starvault.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.starvault.ui.album.AlbumRoute
import com.starvault.ui.files.FilesRoute
import com.starvault.ui.home.HomeRoute
import com.starvault.ui.login.LoginRoute
import com.starvault.ui.player.PlayerRoute
import com.starvault.ui.profile.ProfileRoute
import com.starvault.ui.share.ShareRoute
import com.starvault.ui.transfers.TransfersRoute
import com.starvault.ui.wallpaper.WallpaperRoute

/**
 * App 唯一 NavHost。
 *
 *  - startDestination = Login（T33 后改为持久化 token 决定）
 *  - 4 个 tab 在 T12 的 StarVaultApp() 里判定 showBottomBar（与 NavHost 解耦）
 *  - Files 在 tab (`folderId == null`) 和子目录 (`folderId != null`) 之间共享一个 destination
 */
@Composable
fun StarVaultNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Route.Login, modifier = modifier) {
        composable<Route.Login> {
            LoginRoute(onLoggedIn = {
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            })
        }
        composable<Route.Home>      { HomeRoute(nav = navController) }
        composable<Route.Transfers> { TransfersRoute(nav = navController) }
        composable<Route.Profile>   { ProfileRoute(nav = navController) }

        composable<Route.Files>     { entry -> FilesRoute(args = entry.toRoute(), nav = navController) }
        composable<Route.Album>     { AlbumRoute(nav = navController) }
        composable<Route.Player>    { entry -> PlayerRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Share>     { entry -> ShareRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Wallpaper> { WallpaperRoute(onBack = { navController.popBackStack() }) }
    }
}
