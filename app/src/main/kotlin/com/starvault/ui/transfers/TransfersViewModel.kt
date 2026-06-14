package com.starvault.ui.transfers

import androidx.lifecycle.ViewModel
import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Transfers 屏 ViewModel — Phase 1 mock。
 *
 *  - mockTransfers 与 design/04-transfers.html 的 5 条 1:1 复刻
 *  - 真实接入 115 后：通过 WebSocket / 轮询拉取实时进度
 *  - togglePause / resume / clear / openFolder 各自接到 API
 */
class TransfersViewModel : ViewModel() {

    private val _state = MutableStateFlow<TransfersUiState>(mockState())
    val state: StateFlow<TransfersUiState> = _state.asStateFlow()

    fun selectTab(tab: TransfersTab) {
        _state.value = when (val s = _state.value) {
            is TransfersUiState.Success -> s.copy(activeTab = tab)
            is TransfersUiState.Loading -> s.copy(activeTab = tab)
            is TransfersUiState.Error   -> s.copy(activeTab = tab)
        }
    }

    fun pauseAll() {
        val s = _state.value as? TransfersUiState.Success ?: return
        val paused = s.all.map { if (it.status == TransferStatus.RUNNING) it.copy(status = TransferStatus.PAUSED, speedBps = 0) else it }
        _state.value = s.copy(all = paused, upSpeedBps = 0, downSpeedBps = 0, totalActive = paused.count { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED })
    }

    fun togglePause(transfer: Transfer) {
        val s = _state.value as? TransfersUiState.Success ?: return
        val updated = s.all.map {
            if (it.id != transfer.id) it
            else when (it.status) {
                TransferStatus.RUNNING -> it.copy(status = TransferStatus.PAUSED, speedBps = 0)
                TransferStatus.PAUSED  -> it.copy(status = TransferStatus.RUNNING, speedBps = 5_242_880)
                else -> it
            }
        }
        _state.value = s.copy(
            all = updated,
            upSpeedBps = updated.filter { it.direction == Direction.UP && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps },
            downSpeedBps = updated.filter { it.direction == Direction.DOWN && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps },
        )
    }

    fun clearDone() {
        val s = _state.value as? TransfersUiState.Success ?: return
        val remaining = s.all.filter { it.status != TransferStatus.SUCCESS }
        _state.value = s.copy(
            all = remaining,
            totalDone = 0,
        )
    }

    fun retry(transfer: Transfer) {
        val s = _state.value as? TransfersUiState.Success ?: return
        val updated = s.all.map {
            if (it.id != transfer.id) it
            else it.copy(status = TransferStatus.RUNNING, speedBps = 5_242_880)
        }
        _state.value = s.copy(
            all = updated,
            totalActive = updated.count { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED },
            totalOffline = updated.count { it.status == TransferStatus.FAILED },
        )
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockState(): TransfersUiState.Success {
        val all = listOf(
            // 进行中
            Transfer("t-running-1", "Final.Destination.2026.1080p.mkv", Direction.UP,  totalBytes = 2_340_234_240L, transferredBytes = 1_497_749_913L, speedBps = 8_847_312, status = TransferStatus.RUNNING, startedAt = 1_718_000_000_000L),
            Transfer("t-running-2", "Pink.Floyd.-.Time.flac",              Direction.DOWN, totalBytes = 65_437_696L,     transferredBytes = 15_050_670L,    speedBps = 25_265_824, status = TransferStatus.RUNNING, startedAt = 1_718_001_000_000L),
            Transfer("t-queued",    "毕业设计源码 v2.zip",                 Direction.UP,   totalBytes = 65_142_784L,     transferredBytes = 0L,              speedBps = 0,         status = TransferStatus.PAUSED,  startedAt = 1_718_002_000_000L),
            // 已完成
            Transfer("t-done-1",    "京都樱花 / DSC_4821.jpg",              Direction.UP,   totalBytes = 14_901_248L,     transferredBytes = 14_901_248L,    speedBps = 0,         status = TransferStatus.SUCCESS, startedAt = 1_718_003_000_000L),
            // 失败
            Transfer("t-failed-1",  "Sample.HDR.2160p.Dolby.mkv",          Direction.DOWN, totalBytes = 30_000_000_000L, transferredBytes = 11_400_000_000L, speedBps = 0,         status = TransferStatus.FAILED,  startedAt = 1_718_004_000_000L),
        )
        val totalActive = all.count { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED }
        val totalDone = all.count { it.status == TransferStatus.SUCCESS }
        val totalOffline = all.count { it.status == TransferStatus.FAILED }
        val upSpeedBps = all.filter { it.direction == Direction.UP && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps }
        val downSpeedBps = all.filter { it.direction == Direction.DOWN && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps }
        return TransfersUiState.Success(
            all = all,
            activeTab = TransfersTab.Active,
            totalActive = totalActive,
            totalDone = totalDone,
            totalOffline = totalOffline,
            upSpeedBps = upSpeedBps,
            downSpeedBps = downSpeedBps,
        )
    }
}
