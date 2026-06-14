package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.fixtures.FixturePresets
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
                    all = FixturePresets.transfers(),
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
                    all = FixturePresets.transfers(),
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
                    all = FixturePresets.transfers(),
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
}