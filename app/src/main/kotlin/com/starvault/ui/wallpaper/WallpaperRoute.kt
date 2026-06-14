package com.starvault.ui.wallpaper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route 入口（NavHost 唯一注入点）。
 *
 * NavHost 期望签名 `WallpaperRoute(onBack: () -> Unit)`，按 type-safe nav 走。
 * @param onBack  返回上一屏（一般是 Profile）
 * @param vm     注入 ViewModel
 */
@Composable
fun WallpaperRoute(
    onBack: () -> Unit,
    vm: WallpaperViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    WallpaperScreen(
        state = state,
        onBack = onBack,
        onToggleEngine = vm::onToggleEngine,
        onToggleLiveWallpaper = vm::onToggleLiveWallpaper,
        onPickMode = { vm.openSheet(WallpaperSheetState.ModePicker) },
        onPickAlbum = {
            val s = state as? WallpaperUiState.Success ?: return@WallpaperScreen
            vm.openSheet(WallpaperSheetState.AlbumPicker(s.album.id))
        },
        onPickDisplay = {
            val s = state as? WallpaperUiState.Success ?: return@WallpaperScreen
            vm.openSheet(WallpaperSheetState.DisplayPicker(s.display.value))
        },
        onSwitchNow = vm::switchNow,
        onSheetAlbum = vm::pickAlbum,
        onSheetModeType = vm::pickMode,
        onSheetIntervalValue = vm::updateIntervalValue,
        onSheetIntervalUnit = vm::updateIntervalUnit,
        onSheetDailyTime = vm::updateDailyTime,
        onSheetDisplay = vm::pickDisplay,
        onCloseSheet = vm::closeSheet,
    )
}
