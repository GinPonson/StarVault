package com.starvault.data.downloadworker

/**
 * 跨 VM 桥接的下载元数据 — [DownloadRepository.enqueue] 把 workId + 显示元数据一起
 * 发到 [com.starvault.core.ServiceLocator.downloadWorkTrigger],
 * [com.starvault.ui.transfers.TransfersViewModel.observeDownloadWork] 收到后建占位 + 订阅 WorkInfo。
 *
 * ## 为什么不只用 workId
 *  WorkInfo(WorkManager 2.10.3)不再暴露 inputData,VM 拿不到 [fileName] / [sizeBytes]
 *  用来建占位 entry。把这俩字段塞进 Channel envelope 是最干净的桥接方式 —
 *  Worker 侧零改动,VM 侧拿到完整 snapshot。
 *
 *  字段保持精简:只放 UI 列表 / 通知展示必需的字符串 + 数字。后续如果要传
 *  targetUri / sha1 等,加字段即可,VM 侧兼容(默认参数 + 旧字段忽略)。
 */
data class DownloadWork(
    val workId: java.util.UUID,
    val fileName: String,
    val sizeBytes: Long,
)