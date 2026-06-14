package com.starvault.ui.player

import androidx.lifecycle.ViewModel
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Player 屏 ViewModel — Phase 1 mock 数据（严格按 design/02-player.html 1:1 复刻）。
 *
 * 真实接入 115 后：
 *  - 替换 mockFiles[fileId] → 拉取 FileItem + 缩略图
 *  - replaceMockState → 通过 ExoPlayer/Media3 拿真实 progress / position / duration
 *  - 7 个回调（togglePlay/seekNext/...）映射到 Media3 命令
 *
 * Phase 1 仍暴露完整的命令接口以保 Preview / Paparazzi 闭环。
 */
class PlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(mockReadyState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun togglePlay() {
        val s = _state.value
        if (s is PlayerUiState.Ready) _state.value = s.copy(isPlaying = !s.isPlaying)
    }

    fun seekTo(progress: Float) {
        val s = _state.value
        if (s is PlayerUiState.Ready) {
            val p = progress.coerceIn(0f, 1f)
            _state.value = s.copy(
                progress = p,
                position = formatTime((parseHms(s.duration) * p).toLong()),
            )
        }
    }

    fun changeSpeed(speed: String) {
        val s = _state.value
        if (s is PlayerUiState.Ready) _state.value = s.copy(speedChip = speed)
    }

    fun setTag(label: String) {
        val s = _state.value
        if (s is PlayerUiState.Ready) {
            val existing = s.tags.firstOrNull { it.label == label }
            if (existing != null) {
                _state.value = s.copy(tags = s.tags - existing)
            } else {
                _state.value = s.copy(
                    tags = s.tags + com.starvault.data.model.FileTag(
                        label = label,
                        color = com.starvault.data.model.TagColor.TAG2,
                    ),
                )
            }
        }
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockReadyState(): PlayerUiState.Ready = PlayerUiState.Ready(
        file = FileItem(
            id = "h-02",
            name = "Final.Destination.2026.1080p.mkv",
            type = FileType.VIDEO,
            sizeBytes = 2_340_234_240L,             // 2.18 GB
            durationOrCount = "1:42:08",
            mtime = 1_730_000_000_000L,
            tag = com.starvault.data.model.FileTag("影视", com.starvault.data.model.TagColor.TAG3),
        ),
        progress = 0.32f,                             // 32:14 / 1:42:08
        isPlaying = false,
        tags = PlayerDefaultTags.preset(),
        isAddingTag = true,
        position = "32:14",
        duration = "1:42:08",
        resolution = "1080P",
        codec = "H.265",
        savedAt = "2026-04-12",
        path = "/影视/科幻/",
        uploader = "我",
        sha1 = "9f2c…b4e1",
        downloadCount = 12,
        related = listOf(
            RelatedVideo("r-01", "Final.Destination.2025.1080p.mkv",          "1.92 GB",  "2025-08-22", "1:48:02", 0xFF2F6FEB),
            RelatedVideo("r-02", "Final.Destination.2024.BluRay.REMUX.mkv",   "32.1 GB",  "2024-11-04", "2:14:55", 0xFF9333EA),
            RelatedVideo("r-03", "Final.Destination.Bloodlines.Trailer.mp4", "142 MB",   "2026-05-19", "1:38:12", 0xFFEA580C),
        ),
        qualityChip = "1080P",
        speedChip = "1.0×",
        audioChip = "原声",
        bufferedMb = "8.2",
    )

    /* ─────────────────── 时间工具 ─────────────────── */

    /** 把 "HH:MM:SS" / "MM:SS" 解析成秒。解析失败返回 0。*/
    private fun parseHms(s: String): Long {
        val parts = s.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            else -> 0L
        }
    }

    /** 秒数 → "MM:SS" 或 "HH:MM:SS"。*/
    private fun formatTime(seconds: Long): String {
        val total = seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
