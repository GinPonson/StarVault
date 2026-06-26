package com.starvault.ui.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.data.repository.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Transfers 屏 ViewModel — Phase 5 真实接入 TransferRepository。
 *
 * ## 数据流
 *  - [TransferRepository.all] (StateFlow)  →  通过 `combine + map` 推到 UI state
 *  - UI 状态 = [TransfersUiState.Success] (总是,没有 Loading / Error 占位 — 见 [[no-ui-state-error-placeholders]])
 *  - 写操作(pauseAll / clearDone / retry / togglePause)走 repo,repo 的 StateFlow 自动回流
 *
 * ## 速度聚合
 *  - upSpeedBps = sum(UP RUNNING transfers.speedBps)
 *  - downSpeedBps = sum(DOWN RUNNING transfers.speedBps)
 *  - speedBps 是 UI 估算,不在 repo 里维护 — VM 每次 collect repo 重新算
 *
 * ## observeWork
 *  - [observeWork] 是 Phase 5 引入的入口,UploadRoute enqueue 后调
 *  - 收集 WorkManager.getWorkInfoByIdFlow(workId) → 把 progress 推到 repo
 *  - 这里只暴露接口;具体实现里 [TransfersViewModel.observeWork] 在 ApplicationScope 里 collect
 */
class TransfersViewModel(
    private val repo: TransferRepository = ServiceLocator.transferRepository,
) : ViewModel() {

    private val _activeTab = MutableStateFlow(TransfersTab.Active)

    val state: StateFlow<TransfersUiState> = combine(repo.all, _activeTab) { all, tab ->
        val upSpeedBps = all.filter { it.direction == Direction.UP && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps }
        val downSpeedBps = all.filter { it.direction == Direction.DOWN && it.status == TransferStatus.RUNNING }.sumOf { it.speedBps }
        TransfersUiState.Success(
            all = all,
            activeTab = tab,
            totalActive = all.count { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED },
            totalDone = all.count { it.status == TransferStatus.SUCCESS },
            totalOffline = all.count { it.status == TransferStatus.FAILED },
            upSpeedBps = upSpeedBps,
            downSpeedBps = downSpeedBps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TransfersUiState.Success(
            all = emptyList(),
            activeTab = TransfersTab.Active,
            totalActive = 0,
            totalDone = 0,
            totalOffline = 0,
            upSpeedBps = 0L,
            downSpeedBps = 0L,
        ),
    )

    fun selectTab(tab: TransfersTab) {
        _activeTab.value = tab
    }

    /**
     * 暂停所有 RUNNING 任务。
     *
     * 真实生产里 M2 暂不实现 WorkManager 暂停(M2 spec §2.2 没列),
     * 这里只改 Transfer row 状态显示"已暂停" — 用户切回 Active tab 看到的是 PAUSED row。
     * 真取消由用户点 "Cancel" 触发(M3+)。
     */
    fun pauseAll() {
        val current = repo.all.value
        current.forEach { t ->
            if (t.status == TransferStatus.RUNNING) {
                repo.updateProgress(t.id, transferredBytes = t.transferredBytes, status = TransferStatus.PAUSED, speedBps = 0)
            }
        }
    }

    /**
     * 单条 pause / resume 切换。
     */
    fun togglePause(transfer: Transfer) {
        val current = repo.all.value.find { it.id == transfer.id } ?: return
        when (current.status) {
            TransferStatus.RUNNING -> repo.updateProgress(transfer.id, current.transferredBytes, TransferStatus.PAUSED, 0)
            TransferStatus.PAUSED  -> repo.updateProgress(transfer.id, current.transferredBytes, TransferStatus.RUNNING, 5_242_880L)
            else -> { /* SUCCESS / FAILED: no-op */ }
        }
    }

    /**
     * 清掉 Done tab 上的所有 SUCCESS 条目。
     */
    fun clearDone() {
        repo.clearDone()
    }

    /**
     * 重试 1 条 FAILED 任务。
     *
     * 真实 M2 行为:只是把 status 翻成 RUNNING,WorkManager 不会自动重跑(M2 spec §6 没实现重试入口)。
     * M3+ 在这里调 UploadWorker.enqueue(...) 重投。
     */
    fun retry(transfer: Transfer) {
        repo.updateProgress(
            id = transfer.id,
            transferredBytes = 0L,
            status = TransferStatus.RUNNING,
            speedBps = 5_242_880L,
        )
    }

    /**
     * 观察 1 个 WorkInfo 进度,把 transferred / phase 推到 repo。
     *
     * 调用方(Phase 5 引入 [UploadRoute]):
     *  1. UploadWorker.enqueue → UUID
     *  2. transfersViewModel.observeWork(workId, fileName, totalBytes)
     *     → 内部 add(RUNNING placeholder) + collect WorkInfoByIdFlow
     *  3. 每次 setProgress 推送 transferred / phase
     *  4. phase==DONE → markDone;state==FAILED → markFailed
     *
     * @param onDone 成功后回调(由调用方决定要不要触发 ServiceLocator.filesRefreshTrigger)
     */
    fun observeWork(
        workId: java.util.UUID,
        fileName: String,
        totalBytes: Long,
        onDone: () -> Unit = {},
    ) {
        // 占位 entry — RUNNING,transferredBytes=0
        repo.add(
            Transfer(
                id = workId.toString(),
                fileName = fileName,
                direction = Direction.UP,
                totalBytes = totalBytes,
                transferredBytes = 0L,
                speedBps = 0L,
                status = TransferStatus.RUNNING,
                startedAt = System.currentTimeMillis() / 1000L,
            )
        )
        // 收集 WorkInfo(在 viewModelScope 走 Application 上下文,需要 WorkManager 注入)
        viewModelScope.launch {
            androidx.work.WorkManager.getInstance(ServiceLocator.appContext)
                .getWorkInfoByIdFlow(workId)
                .collect { info ->
                    if (info == null) return@collect
                    val phase = info.progress.getString(com.starvault.data.uploadworker.UploadWorker.ProgressKey.Phase)
                    val transferred = info.progress.getLong(com.starvault.data.uploadworker.UploadWorker.ProgressKey.Transferred, 0L)
                    when {
                        phase == com.starvault.data.uploadworker.UploadWorker.Phase.DONE -> {
                            repo.markDone(workId.toString())
                            // Phase 6:上传完成 → 通知 FilesViewModel 重拉当前目录,
                            // 新上传的文件立即出现在文件列表(用户不用手动 pull-to-refresh)。
                            // tryEmit 不挂起、bufferCapacity=4 不丢信号。
                            ServiceLocator.filesRefreshTrigger.tryEmit(Unit)
                            onDone()
                        }
                        info.state == androidx.work.WorkInfo.State.FAILED -> {
                            repo.markFailed(workId.toString(), info.outputData.getString("error") ?: "上传失败")
                        }
                        phase == com.starvault.data.uploadworker.UploadWorker.Phase.RUNNING -> {
                            repo.updateProgress(workId.toString(), transferredBytes = transferred)
                        }
                    }
                }
        }
    }
}
