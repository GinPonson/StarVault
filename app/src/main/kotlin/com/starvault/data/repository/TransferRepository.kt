package com.starvault.data.repository

import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 内存版 Transfer 仓库 — 进程级单例,持有 [Transfer] 列表 + 状态变更入口。
 *
 * ## 角色
 *  - 写入侧:[UploadWorker] 通过 WorkManager setProgress → [TransfersViewModel] 观察 → 调用本仓库
 *    `updateProgress` / `markDone` / `markFailed` 方法
 *  - 读取侧:[TransfersViewModel] 把 [all] 转成 [TransfersUiState.Success] 给 Compose 渲染
 *
 * ## 持久化
 *  - M2 不做持久化(进程死亡 = Transfer 列表丢,符合 M2 范围 — spec §2.1 限制)
 *  - M3+ 如果需要跨进程保留,可以加 Room / DataStore
 *
 * ## 并发
 *  - 全部修改走 [MutableStateFlow.update](原子 CAS),concurrent 调用无丢失
 *  - 单条 list 替换不阻塞 — StateFlow 在新订阅者到达时 push 一次
 */
class TransferRepository {

    private val _all = MutableStateFlow<List<Transfer>>(emptyList())

    /** 当前所有 transfer,订阅后立即收到 initial value(冷启动 + 热更都行)。 */
    val all: StateFlow<List<Transfer>> = _all.asStateFlow()

    /**
     * 添加 1 条 transfer。
     *
     * - 重复 id(同 workId 重提):覆盖原条目(M2 不会发生,workId UUID 唯一,但防御写)
     * - 顺序保持:新条目追加到末尾
     */
    fun add(transfer: Transfer) {
        _all.update { current ->
            val filtered = current.filterNot { it.id == transfer.id }
            filtered + transfer
        }
    }

    /**
     * 更新某条 transfer 的 transferredBytes(以及可选的 status / speedBps)。
     *
     * @param status 默认 null 表示保留原 status;传新值(如 PAUSED)用于暂停场景
     * @param speedBps 默认 null 表示保留原 speedBps
     */
    fun updateProgress(
        id: String,
        transferredBytes: Long,
        status: TransferStatus? = null,
        speedBps: Long? = null,
    ) {
        _all.update { current ->
            current.map { t ->
                if (t.id != id) t
                else t.copy(
                    transferredBytes = transferredBytes,
                    status = status ?: t.status,
                    speedBps = speedBps ?: t.speedBps,
                )
            }
        }
    }

    /**
     * 标记某条 transfer 完成 — status = SUCCESS,transferredBytes = totalBytes。
     *
     * - 调用方应在 phase == DONE 时调(从 [UploadWorker.Phase.DONE] 推断)
     * - markDone 后 workId 仍然在列表(用于 Done tab 展示)
     * - 用户可以 `clearDone()` 清掉
     */
    fun markDone(id: String) {
        _all.update { current ->
            current.map { t ->
                if (t.id != id) t
                else t.copy(
                    status = TransferStatus.SUCCESS,
                    transferredBytes = t.totalBytes,
                    speedBps = 0L,
                )
            }
        }
    }

    /**
     * 标记某条 transfer 失败 — status = FAILED,清零 speedBps。
     *
     * @param errorMsg 错误描述(给 Debug 用,UI 不展示 — UI 走 ToastBus 已经弹了)
     */
    fun markFailed(id: String, errorMsg: String) {
        _all.update { current ->
            current.map { t ->
                if (t.id != id) t
                else t.copy(
                    status = TransferStatus.FAILED,
                    speedBps = 0L,
                )
            }
        }
    }

    /**
     * 清掉所有 SUCCESS 条目(用户点 "Clear" 按钮时)。
     */
    fun clearDone() {
        _all.update { current -> current.filter { it.status != TransferStatus.SUCCESS } }
    }

    /**
     * 清掉所有 FAILED 条目(用户点 "Clear" 按钮时 — Offlline tab 上的 Clear)。
     */
    fun clearFailed() {
        _all.update { current -> current.filter { it.status != TransferStatus.FAILED } }
    }

    /**
     * 移除指定 id(取消/删除单条)— M3+ 用,M2 暂未在 UI 暴露。
     */
    fun remove(id: String) {
        _all.update { current -> current.filterNot { it.id == id } }
    }
}
