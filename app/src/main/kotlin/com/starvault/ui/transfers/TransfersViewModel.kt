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
 * ## observeWork / observeDownloadWork
 *  - [observeWork] 是 Phase 5 引入的入口,UploadRoute enqueue 后调
 *  - [observeDownloadWork] 是 M3 引入的入口;由 [TransfersViewModel.init] 在
 *    [ServiceLocator.downloadWorkFlow] 上订阅 — DownloadRepository.enqueue 时
 *    `downloadWorkTrigger.trySend(workId)`,VM 收到 UUID 后调 observeDownloadWork
 *  - 收集 WorkManager.getWorkInfoByIdFlow(workId) → 把 progress 推到 repo
 *  - 这里只暴露接口;具体实现里 collect 跑在 appScope(进程级,nav pop 不死)
 */
class TransfersViewModel(
    private val repo: TransferRepository = ServiceLocator.transferRepository,
) : ViewModel() {

    init {
        // M3:订阅下载 work 触发器 — FilesVM.downloadEntry → DownloadRepository.enqueue
        //   → ServiceLocator.downloadWorkTrigger.trySend(DownloadWork) → 此处 collect
        //   → observeDownloadWork(envelope) 订阅 WorkInfo,推到 TransferRepository
        //
        // 与 M2 UploadRoute 不同:upload 是同步 enqueue 后立刻 observeWork(uuid, fileName, totalBytes),
        // download 是异步触发(用户点 Files 屏 "···" → 下载),所以走 Channel 桥接。
        //
        // appScope 而不是 viewModelScope:TransfersViewModel 是 nav-scoped,
        // nav pop 后 viewModelScope cancel → collect 死 → WorkInfo 不再消费。
        // appScope 跨 nav pop 不死,Downloads 屏切回仍能看到进度更新。
        ServiceLocator.appScope.launch {
            ServiceLocator.downloadWorkFlow.collect { envelope ->
                observeDownloadWork(envelope)
            }
        }
    }

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
     * 调用方(Phase 5 引入 [com.starvault.ui.upload.UploadRoute]):
     *  1. UploadWorker.enqueue → UUID
     *  2. transfersViewModel.observeWork(workId, fileName, totalBytes)
     *     → 内部 add(RUNNING placeholder) + collect WorkInfoByIdFlow
     *  3. 每次 setProgress 推送 transferred / phase
     *  4. phase==DONE → markDone + filesRefreshTrigger.trySend
     *  5. state==FAILED → markFailed
     *
     * ## Scope 选型 — 为什么 appScope 不是 viewModelScope
     *  viewModelScope 在 nav pop 后 cancel(`TransfersViewModel` 是 nav-scoped:
     *  `composable<Route.Transfers>` 的 ViewModelStoreOwner 是 NavBackStackEntry)。
     *  如果 collect 跑在 viewModelScope,用户在上传过程中离开 Transfers,
     *  collect 协程被取消 → `phase == DONE` 永远不被消费 → `repo.markDone` 不触发
     *  → `filesRefreshTrigger.trySend` 不触发 → Files 永远收不到刷新信号。
     *
     *  改用 [ServiceLocator.appScope](进程级 `CoroutineScope(SupervisorJob())`),
     *  collect 跨 nav pop 不死,Files 自动刷新链路完整。
     *
     * ## Terminal state 显式退出
     *  `WorkManager.getWorkInfoByIdFlow` 是 StateFlow-backed Flow,terminal state(SUCCEEDED /
     *  FAILED / CANCELLED)后**不会自 complete**。如果不显式 `return@collect`,
     *  collect 会持续监听同一个 terminal state,虽然没副作用(when 都不命中)但浪费资源。
     *  DONE / FAILED / isFinished 三处 terminal 全部 return@collect。
     */
    fun observeWork(
        workId: java.util.UUID,
        fileName: String,
        totalBytes: Long,
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
        // 收集 WorkInfo(进程级 appScope,nav pop 不死)
        ServiceLocator.appScope.launch {
            androidx.work.WorkManager.getInstance(ServiceLocator.appContext)
                .getWorkInfoByIdFlow(workId)
                .collect { info ->
                    if (info == null) return@collect
                    val phase = info.progress.getString(com.starvault.data.uploadworker.UploadWorker.ProgressKey.Phase)
                    val transferred = info.progress.getLong(com.starvault.data.uploadworker.UploadWorker.ProgressKey.Transferred, 0L)
                    when {
                        phase == com.starvault.data.uploadworker.UploadWorker.Phase.DONE -> {
                            repo.markDone(workId.toString())
                            // Phase 6:上传完成 → 通知 FilesViewModel 重拉当前目录。
                            // Channel(UNLIMITED) 的 trySend 永不丢:FilesVM 不在线时 buffer,
                            // 新 collector subscribe 后从头部消费。
                            ServiceLocator.filesRefreshTrigger.trySend(Unit)
                            return@collect
                        }
                        info.state == androidx.work.WorkInfo.State.FAILED -> {
                            repo.markFailed(workId.toString(), info.outputData.getString("error") ?: "上传失败")
                            return@collect
                        }
                        info.state.isFinished -> {
                            // CANCELLED / 其他 terminal 不走 phase 也不走 FAILED
                            return@collect
                        }
                        phase == com.starvault.data.uploadworker.UploadWorker.Phase.RUNNING -> {
                            repo.updateProgress(workId.toString(), transferredBytes = transferred)
                        }
                    }
                }
        }
    }

    /**
     * M3 下载入口 — 镜像 [observeWork],处理 [com.starvault.data.downloadworker.DownloadWorker]
     * 的 setProgress 信号。
     *
     * ## 与 observeWork 的差异
     *  - direction = Direction.DOWN
     *  - phase DONE 不调 `filesRefreshTrigger.trySend`(下载不动远端列表)
     *  - fileName / totalBytes 从 envelope [DownloadWork] 直接拿(WorkManager 2.10.3
     *    WorkInfo 不暴露 inputData),不需要 WorkManager 输入数据
     *  - 入口是 Channel 桥接([ServiceLocator.downloadWorkFlow]),不是 UploadRoute 同步调
     *
     * ## 多并发安全
     *  每个 workId 独立 collect,互不干扰。
     *
     * ## Terminal 退出 + 占位时机
     *  - 入口直接调 repo.add() 占位(envelope 已带 fileName / totalBytes)
     *  - 之后 setProgress 阶段持续 updateProgress
     *  - 终态(DONE / FAILED / CANCELLED)→ markDone / markFailed + return@collect
     */
    fun observeDownloadWork(envelope: com.starvault.data.downloadworker.DownloadWork) {
        val workId = envelope.workId
        val downloadPhase = com.starvault.data.downloadworker.DownloadWorker.Phase
        val progressKey = com.starvault.data.downloadworker.DownloadWorker.ProgressKey

        // 占位 entry — 用 envelope 元数据建,不等 WorkInfo 首帧
        repo.add(
            Transfer(
                id = workId.toString(),
                fileName = envelope.fileName,
                direction = Direction.DOWN,
                totalBytes = envelope.sizeBytes,
                transferredBytes = 0L,
                speedBps = 0L,
                status = TransferStatus.RUNNING,
                startedAt = System.currentTimeMillis() / 1000L,
            ),
        )

        ServiceLocator.appScope.launch {
            androidx.work.WorkManager.getInstance(ServiceLocator.appContext)
                .getWorkInfoByIdFlow(workId)
                .collect { info ->
                    if (info == null) return@collect
                    val phase = info.progress.getString(progressKey.Phase)
                    val transferred = info.progress.getLong(progressKey.Transferred, 0L)
                    when {
                        phase == downloadPhase.DONE -> {
                            repo.markDone(workId.toString())
                            // 注意:下载成功**不**触发 filesRefreshTrigger — 远端列表无变化
                            return@collect
                        }
                        phase == downloadPhase.FAILED || info.state == androidx.work.WorkInfo.State.FAILED -> {
                            val msg = info.outputData.getString("error") ?: "下载失败"
                            repo.markFailed(workId.toString(), msg)
                            return@collect
                        }
                        phase == downloadPhase.CANCELED || info.state == androidx.work.WorkInfo.State.CANCELLED -> {
                            // CANCELED = WorkManager backoff 重试中(spec §6.2);当前 entry 标 FAILED,
                            // 重试成功会重新走 RUNNING → DONE 路径
                            repo.markFailed(workId.toString(), "已取消(将自动重试)")
                            return@collect
                        }
                        info.state.isFinished -> {
                            // 其他 terminal 不命中以上分支 — 静默退出
                            return@collect
                        }
                        phase == downloadPhase.RUNNING -> {
                            repo.updateProgress(workId.toString(), transferredBytes = transferred)
                        }
                    }
                }
        }
    }
}
