package com.starvault.ui.transfers

import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus

/**
 * Transfers 屏 UiState（对应 design/04-transfers.html）。
 *
 *  - Loading   : 占位
 *  - Success   : 全部 transfer 列表（按 status 自动分组给 3 个 tab 渲染）
 *  - Error     : 拉取失败(Phase 1 mock 不再使用,改走 ToastBus 错误提示)
 *
 * 三个 tab 状态过滤（与 HTML `.tab` 一一对应）：
 *  - Active   : RUNNING + PAUSED  （"进行中"，含暂停的以便用户恢复）
 *  - Done     : SUCCESS
 *  - Offline  : FAILED
 */
sealed interface TransfersUiState {

    val all: List<Transfer>
    val activeTab: TransfersTab
    val totalActive: Int
    val totalDone: Int
    val totalOffline: Int
    val upSpeedBps: Long
    val downSpeedBps: Long

    data class Loading(
        override val activeTab: TransfersTab = TransfersTab.Active,
    ) : TransfersUiState {
        override val all: List<Transfer> = emptyList()
        override val totalActive: Int = 0
        override val totalDone: Int = 0
        override val totalOffline: Int = 0
        override val upSpeedBps: Long = 0L
        override val downSpeedBps: Long = 0L
    }

    data class Success(
        override val all: List<Transfer>,
        override val activeTab: TransfersTab = TransfersTab.Active,
        override val totalActive: Int = 0,
        override val totalDone: Int = 0,
        override val totalOffline: Int = 0,
        override val upSpeedBps: Long = 0L,
        override val downSpeedBps: Long = 0L,
    ) : TransfersUiState
}

enum class TransfersTab(val label: String) {
    Active("进行中"),
    Done("已完成"),
    Offline("已离线"),
}

/** 一条 transfer 的扁平化 UI 视图（与 HTML `.item` 一一对应）。*/
data class TransferRow(
    val id: String,
    val name: String,
    val direction: Direction,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBps: Long,
    val status: TransferStatus,
    val startedAt: Long,
) {
    val progress: Float
        get() = if (totalBytes <= 0) 0f
        else (transferredBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
}
