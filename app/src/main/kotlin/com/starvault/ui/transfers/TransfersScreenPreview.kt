package com.starvault.ui.transfers

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Transfers 屏 Preview — 3 个 case：
 *  1. Ready/Active  (默认)  : 5 条混合状态
 *  2. Ready/Done           : 仅 SUCCESS 的 1 条
 *  3. Loading              : 占位
 */
@Preview(name = "Transfers/Active", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun TransfersActivePreview() = StarVaultTheme {
    TransfersScreen(
        state = transfersPreviewReady(),
        onSearch = {},
        onClear = {},
        onTab = {},
        onPauseAll = {},
        onPause = {},
        onMore = {},
        onRetry = {},
    )
}

@Preview(name = "Transfers/Done",   showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun TransfersDonePreview() = StarVaultTheme {
    TransfersScreen(
        state = transfersPreviewReady().copy(activeTab = TransfersTab.Done),
        onSearch = {},
        onClear = {},
        onTab = {},
        onPauseAll = {},
        onPause = {},
        onMore = {},
        onRetry = {},
    )
}

@Preview(name = "Transfers/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun TransfersLoadingPreview() = StarVaultTheme {
    TransfersScreen(
        state = TransfersUiState.Loading(),
        onSearch = {},
        onClear = {},
        onTab = {},
        onPauseAll = {},
        onPause = {},
        onMore = {},
        onRetry = {},
    )
}

private fun transfersPreviewReady(): TransfersUiState.Success = TransfersUiState.Success(
    all = listOf(
        com.starvault.data.model.Transfer("r1", "Final.Destination.2026.1080p.mkv", com.starvault.data.model.Direction.UP, 2_340_234_240L, 1_497_749_913L, 8_847_312L, com.starvault.data.model.TransferStatus.RUNNING, 1_718_000_000_000L),
        com.starvault.data.model.Transfer("r2", "Pink.Floyd.-.Time.flac",             com.starvault.data.model.Direction.DOWN, 65_437_696L, 15_050_670L, 25_265_824L, com.starvault.data.model.TransferStatus.RUNNING, 1_718_001_000_000L),
        com.starvault.data.model.Transfer("r3", "毕业设计源码 v2.zip",                com.starvault.data.model.Direction.UP, 65_142_784L, 0L, 0L, com.starvault.data.model.TransferStatus.PAUSED, 1_718_002_000_000L),
        com.starvault.data.model.Transfer("r4", "京都樱花 / DSC_4821.jpg",             com.starvault.data.model.Direction.UP, 14_901_248L, 14_901_248L, 0L, com.starvault.data.model.TransferStatus.SUCCESS, 1_718_003_000_000L),
        com.starvault.data.model.Transfer("r5", "Sample.HDR.2160p.Dolby.mkv",         com.starvault.data.model.Direction.DOWN, 30_000_000_000L, 11_400_000_000L, 0L, com.starvault.data.model.TransferStatus.FAILED, 1_718_004_000_000L),
    ),
    activeTab = TransfersTab.Active,
    totalActive = 3,
    totalDone = 1,
    totalOffline = 1,
    upSpeedBps = 8_847_312L,
    downSpeedBps = 25_265_824L,
)
