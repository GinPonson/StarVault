package com.starvault.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Player 屏 Preview — 3 个 case：
 *  1. Ready（默认）       ：UI 1:1 与 design/02-player.html
 *  2. Playing              : isPlaying=true，中心按钮显示 pause glyph
 *  3. Loading              : 画布中央显示 "载入中…"
 *
 * widthDp/heightDp = 412x900，与 design HTML device frame 一致。
 */
@Preview(name = "Player/Ready",   showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun PlayerReadyPreview() = StarVaultTheme {
    PlayerScreen(
        state = playerPreviewReady(),
        onBack = {},
        onTogglePlay = {},
        onSeek = {},
        onSpeed = {},
        onDownload = {},
        onTransfer = {},
        onShare = {},
        onAddTag = {},
        onTagClick = {},
        onRelated = {},
    )
}

@Preview(name = "Player/Playing", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun PlayerPlayingPreview() = StarVaultTheme {
    PlayerScreen(
        state = playerPreviewReady().copy(isPlaying = true),
        onBack = {},
        onTogglePlay = {},
        onSeek = {},
        onSpeed = {},
        onDownload = {},
        onTransfer = {},
        onShare = {},
        onAddTag = {},
        onTagClick = {},
        onRelated = {},
    )
}

@Preview(name = "Player/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun PlayerLoadingPreview() = StarVaultTheme {
    PlayerScreen(
        state = PlayerUiState.Loading(),
        onBack = {},
        onTogglePlay = {},
        onSeek = {},
        onSpeed = {},
        onDownload = {},
        onTransfer = {},
        onShare = {},
        onAddTag = {},
        onTagClick = {},
        onRelated = {},
    )
}

private fun playerPreviewReady(): PlayerUiState.Ready = PlayerUiState.Ready(
    file = com.starvault.data.model.FileItem(
        "h-02",
        "Final.Destination.2026.1080p.mkv",
        com.starvault.data.model.FileType.VIDEO,
        sizeBytes = 2_340_234_240L,
        durationOrCount = "1:42:08",
        mtime = 1_730_000_000_000L,
    ),
    progress = 0.32f,
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
        RelatedVideo("r-01", "Final.Destination.2025.1080p.mkv",          "1.92 GB", "2025-08-22", "1:48:02",  0xFF2F6FEB),
        RelatedVideo("r-02", "Final.Destination.2024.BluRay.REMUX.mkv",   "32.1 GB", "2024-11-04", "2:14:55",  0xFF9333EA),
        RelatedVideo("r-03", "Final.Destination.Bloodlines.Trailer.mp4", "142 MB",  "2026-05-19", "1:38:12",  0xFFEA580C),
    ),
    qualityChip = "1080P",
    speedChip = "1.0×",
    audioChip = "原声",
    bufferedMb = "8.2",
)
