package com.starvault.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.starvault.data.repository.AuthState
import com.starvault.ui.album.AlbumRoute
import com.starvault.ui.debug.ThumbStateLab
import com.starvault.ui.files.FilesRoute
import com.starvault.ui.home.HomeRoute
import com.starvault.ui.login.LoginRoute
import com.starvault.ui.player.PlayerRoute
import com.starvault.ui.preview.PreviewAudioRoute
import com.starvault.ui.preview.PreviewImageRoute
import com.starvault.ui.preview.PreviewVideoRoute
import com.starvault.ui.profile.ProfileRoute
import com.starvault.ui.search.SearchRoute
import com.starvault.ui.share.ShareRoute
import com.starvault.ui.transfers.TransfersRoute
import com.starvault.ui.wallpaper.WallpaperRoute

/**
 * App 唯一 NavHost。
 *
 *  - startDestination 由 [authState] 动态决定：
 *      Unauthenticated → Login
 *      Authenticated   → Home
 *  - 4 个 tab 在 StarVaultApp() 里判定 showBottomBar（与 NavHost 解耦）
 *  - Files 在 tab (`folderId == null`) 和子目录 (`folderId != null`) 之间共享一个 destination
 */
@Composable
fun StarVaultNavHost(
    navController: NavHostController,
    authState: AuthState,
    modifier: Modifier = Modifier,
) {
    val startDestination: Any = when (authState) {
        AuthState.Unauthenticated -> Route.Login
        is AuthState.Authenticated -> Route.Home
    }
    NavHost(navController, startDestination = startDestination, modifier = modifier) {
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
        // Preview 屏：黑底全屏预览 IMAGE / VIDEO；不显示 bottom-nav
        composable<Route.PreviewImage> { entry -> PreviewImageRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.PreviewVideo> { entry -> PreviewVideoRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }, nav = navController) }
        composable<Route.PreviewAudio> { entry -> PreviewAudioRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }, nav = navController) }
        composable<Route.Album>     { AlbumRoute(nav = navController) }
        composable<Route.Player>    { entry -> PlayerRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Share>     { entry -> ShareRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Wallpaper> { WallpaperRoute(onBack = { navController.popBackStack() }) }
        composable<Route.ThumbLab>  { ThumbStateLab() }
        // 搜索屏（Files 屏的搜索入口跳转）：全屏覆盖，不显示 bottom-nav
        composable<Route.Search>   { entry -> SearchRoute(args = entry.toRoute(), nav = navController) }
    }
}
