package com.starvault.ui.wallpaper

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wallpaper 屏 ViewModel — Phase 1 mock。
 *
 * 真实接入 115 后：
 *  - state ← SharedPreferences（"__wallpaperEngine"）+ /album/list
 *  - onToggleEngine / onToggleLiveWallpaper ← 写 SP
 *  - openSheet/closeSheet/pickMode/pickDisplay/pickAlbum ← 本地态
 *  - 真实持久化在 24+ 之后做（Phase 1 全部 in-memory）
 */
class WallpaperViewModel : ViewModel() {

    private val _state = MutableStateFlow<WallpaperUiState>(mockState())
    val state: StateFlow<WallpaperUiState> = _state.asStateFlow()

    fun onToggleEngine() {
        val s = _state.value as? WallpaperUiState.Success ?: return
        _state.value = s.copy(enabled = !s.enabled)
    }

    fun onToggleLiveWallpaper() {
        val s = _state.value as? WallpaperUiState.Success ?: return
        _state.value = s.copy(liveWallpaper = !s.liveWallpaper)
    }

    fun openSheet(sheet: WallpaperSheetState) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        _state.value = s.copy(sheet = sheet)
    }

    fun closeSheet() {
        val s = _state.value as? WallpaperUiState.Success ?: return
        _state.value = s.copy(sheet = WallpaperSheetState.Closed)
    }

    fun pickAlbum(id: String) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        val target = s.albumOptions.firstOrNull { it.id == id }
            ?: s.childrenOf.values.flatten().firstOrNull { it.id == id }
            ?: return
        _state.value = s.copy(album = target, sheet = WallpaperSheetState.Closed)
    }

    fun pickMode(type: String) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        val next: Mode = when (type) {
            "unlock"   -> Mode.Unlock
            "manual"   -> Mode.Manual
            "interval" -> s.mode.let { Mode.Interval(6, IntervalUnit.HOUR) }
            "daily"    -> Mode.Daily("09:00")
            else       -> return
        }
        _state.value = s.copy(mode = next)
        // 切到 daily/interval 时保持 sheet 打开（让用户继续编辑数字/时间）
        if (type == "unlock" || type == "manual") {
            _state.value = s.copy(mode = next, sheet = WallpaperSheetState.Closed)
        }
    }

    fun updateIntervalValue(value: Int) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        val v = value.coerceIn(1, 999)
        val unit = (s.mode as? Mode.Interval)?.unit ?: IntervalUnit.HOUR
        _state.value = s.copy(mode = Mode.Interval(v, unit))
    }

    fun updateIntervalUnit(unit: IntervalUnit) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        val v = (s.mode as? Mode.Interval)?.value ?: 6
        _state.value = s.copy(mode = Mode.Interval(v, unit))
    }

    fun updateDailyTime(time: String) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        _state.value = s.copy(mode = Mode.Daily(time.ifEmpty { "09:00" }))
    }

    fun pickDisplay(value: String) {
        val s = _state.value as? WallpaperUiState.Success ?: return
        val next = DisplayMode.entries.firstOrNull { it.value == value } ?: return
        _state.value = s.copy(display = next, sheet = WallpaperSheetState.Closed)
    }

    fun switchNow() {
        val s = _state.value as? WallpaperUiState.Success ?: return
        if (!s.enabled) return
        // Phase 1 stub：切到下一张待真实引擎接入
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockState(): WallpaperUiState.Success {
        val albums = listOf(
            AlbumRef("mine",   "我的相册",   Color(0xFF2F6FEB), 1247),
            AlbumRef("travel", "旅行 2025", Color(0xFF16A34A), 328),
            AlbumRef("family", "家庭",      Color(0xFFF59E0B), 156),
            AlbumRef("work",   "工作",      Color(0xFF8B5CF6), 47),
            AlbumRef("fav",    "收藏",      Color(0xFFEF4444), 23),
        )
        val children = mapOf(
            "mine" to listOf(
                AlbumRef("mine-2025", "2025", Color(0xFF2F6FEB), 328),
                AlbumRef("mine-2024", "2024", Color(0xFF2F6FEB), 612),
            ),
            "family" to listOf(
                AlbumRef("family-baby", "宝宝成长", Color(0xFFEC4899), 89),
            ),
        )
        return WallpaperUiState.Success(
            enabled = false,
            mode = Mode.Interval(6, IntervalUnit.HOUR),
            album = albums.first { it.id == "travel" },
            display = DisplayMode.CROP,
            liveWallpaper = false,
            albumOptions = albums,
            childrenOf = children,
            sheet = WallpaperSheetState.Closed,
        )
    }
}
