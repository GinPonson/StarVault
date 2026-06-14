package com.starvault.data.model

import kotlinx.serialization.Serializable

/**
 * 一条传输任务（上传或下载）。
 *
 *  - direction       UP / DOWN
 *  - totalBytes      总大小
 *  - transferredBytes 已传输
 *  - speedBps        字节/秒（前端换算成 KB/s / MB/s）
 *  - status          RUNNING / PAUSED / SUCCESS / FAILED
 *  - startedAt       Unix ms
 */
@Serializable
data class Transfer(
    val id: String,
    val fileName: String,
    val direction: Direction,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBps: Long,
    val status: TransferStatus,
    val startedAt: Long,
)

@Serializable
enum class Direction      { UP, DOWN }
@Serializable
enum class TransferStatus { RUNNING, PAUSED, SUCCESS, FAILED }
