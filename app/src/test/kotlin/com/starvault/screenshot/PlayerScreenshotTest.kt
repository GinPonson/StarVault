package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileType
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.player.PlayerScreen
import com.starvault.ui.player.PlayerUiState
import org.junit.Rule
import org.junit.Test

/**
 * Player 屏 Paparazzi 回归基线 — 与 design/02-player.html 对齐。
 *
 *  - playing : 播放中（约 28% 进度，已缓冲 142MB）
 *  - paused  : 暂停态（progress=0）
 *  - error   : 视频元数据拉取失败
 */
class PlayerScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private val sampleFile = FileItem(
        id = "v-001",
        name = "Interstellar.2014.BluRay.1080p.mkv",
        type = FileType.VIDEO,
        sizeBytes = 8_240_000_000L,
        durationOrCount = "2:49:01",
        mtime = System.currentTimeMillis(),
    )

    @Test fun player_playing() = paparazzi.snapshot {
        StarVaultTheme {
            PlayerScreen(
                state = PlayerUiState.Ready(
                    file = sampleFile,
                    progress = 0.28f,
                    isPlaying = true,
                    position = "00:47:21",
                    duration = "2:49:01",
                    resolution = "1080P",
                    codec = "H.265",
                    bufferedMb = "142.6",
                ),
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
    }

    @Test fun player_paused() = paparazzi.snapshot {
        StarVaultTheme {
            PlayerScreen(
                state = PlayerUiState.Ready(
                    file = sampleFile,
                    progress = 0f,
                    isPlaying = false,
                ),
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
    }

    @Test fun player_error() = paparazzi.snapshot {
        StarVaultTheme {
            PlayerScreen(
                state = PlayerUiState.Error(message = "视频元数据获取失败"),
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
    }
}