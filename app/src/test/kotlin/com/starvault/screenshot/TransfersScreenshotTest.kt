package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.transfers.TransfersScreen
import com.starvault.ui.transfers.TransfersTab
import com.starvault.ui.transfers.TransfersUiState
import org.junit.Rule
import org.junit.Test

/**
 * Transfers 屏 Paparazzi 回归基线 — 与 design/04-transfers.html 对齐。
 *
 *  - active : 进行中 tab（含 RUNNING + PAUSED 两条 transfer）
 *  - done   : 已完成 tab（SUCCESS）
 *  - offline: 已离线 tab（FAILED）
 */
class TransfersScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    @Test fun transfers_active() = paparazzi.snapshot {
        StarVaultTheme {
            TransfersScreen(
                state = TransfersUiState.Success(
                    all = mockTransfers(),
                    activeTab = TransfersTab.Active,
                    upSpeedBps = 524288,
                    downSpeedBps = 5242880,
                ),
                onSearch = {},
                onClear = {},
                onTab = {},
                onPauseAll = {},
                onPause = {},
                onMore = {},
                onRetry = {},
            )
        }
    }

    @Test fun transfers_done() = paparazzi.snapshot {
        StarVaultTheme {
            TransfersScreen(
                state = TransfersUiState.Success(
                    all = mockTransfers(),
                    activeTab = TransfersTab.Done,
                ),
                onSearch = {},
                onClear = {},
                onTab = {},
                onPauseAll = {},
                onPause = {},
                onMore = {},
                onRetry = {},
            )
        }
    }

    @Test fun transfers_offline() = paparazzi.snapshot {
        StarVaultTheme {
            TransfersScreen(
                state = TransfersUiState.Success(
                    all = mockTransfers(),
                    activeTab = TransfersTab.Offline,
                ),
                onSearch = {},
                onClear = {},
                onTab = {},
                onPauseAll = {},
                onPause = {},
                onMore = {},
                onRetry = {},
            )
        }
    }

    /**
     * Phase 6 上传中状态 — Active tab 展示一个真实在跑的 UP RUNNING + 高上行速率。
     *
     * 与 [transfers_active] 的差别:
     *  - upSpeedBps 设大(5 MB/s)— 表示视频/批量文件上传的典型场景
     *  - downSpeedBps = 0(用户没在下载)
     *  - 多 1 条 vacation.mp4 在跑(覆盖列表更密)
     */
    @Test fun transfers_uploading() = paparazzi.snapshot {
        StarVaultTheme {
            TransfersScreen(
                state = TransfersUiState.Success(
                    all = mockTransfers(),
                    activeTab = TransfersTab.Active,
                    upSpeedBps = 5_242_880L,
                    downSpeedBps = 0L,
                ),
                onSearch = {},
                onClear = {},
                onTab = {},
                onPauseAll = {},
                onPause = {},
                onMore = {},
                onRetry = {},
            )
        }
    }

    // 5 条覆盖 RUNNING / SUCCESS / PAUSED / FAILED 全状态 + Phase 6 新增 UP RUNNING
    private fun mockTransfers(): List<Transfer> = listOf(
        Transfer("t01", "movie.mp4",    Direction.DOWN, 2_147_483_648, 1_073_741_824, 5_242_880,  TransferStatus.RUNNING, 1_718_100_000),
        Transfer("t02", "song.flac",    Direction.UP,   52_428_800,     52_428_800,     0,          TransferStatus.SUCCESS, 1_718_000_000),
        Transfer("t03", "doc.pdf",      Direction.DOWN, 5_242_880,      2_621_440,      0,          TransferStatus.PAUSED,  1_717_900_000),
        Transfer("t04", "corrupt.avi",  Direction.DOWN, 209_715_200,    52_428_800,     0,          TransferStatus.FAILED, 1_717_800_000),
        Transfer("t05", "vacation.mp4", Direction.UP,   104_857_600,    47_244_800,     5_242_880,  TransferStatus.RUNNING, 1_718_200_000),
    )
}