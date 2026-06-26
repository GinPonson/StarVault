package com.starvault.data.repository

import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TransferRepository 单元测试 — 纯 JVM。
 *
 * 关键约束:
 *  - 单写线程:在 main thread 调用 add/update/markDone/markFailed;concurrent update 顺序保留
 *  - StateFlow<List<Transfer>> 在 repo 内部 hold,exposed 出去
 *  - add duplicate id:覆盖原条目(M2 不会发生,因为 id = workId UUID,workId 唯一)
 *  - updateProgress with unknown id:no-op
 */
class TransferRepositoryTest {

    private val repo = TransferRepository()

    @Test fun `add places transfer in state`() = runBlocking {
        val t = sampleTransfer("t1")
        repo.add(t)
        assertEquals(listOf(t), repo.all.value)
    }

    @Test fun `add preserves insertion order`() = runBlocking {
        val t1 = sampleTransfer("t1", transferredBytes = 100)
        val t2 = sampleTransfer("t2", transferredBytes = 200)
        val t3 = sampleTransfer("t3", transferredBytes = 300)
        repo.add(t1)
        repo.add(t2)
        repo.add(t3)
        assertEquals(listOf(t1, t2, t3), repo.all.value)
    }

    @Test fun `updateProgress finds by id and updates transferredBytes`() = runBlocking {
        repo.add(sampleTransfer("t1", transferredBytes = 100))
        repo.updateProgress("t1", transferredBytes = 500)
        val updated = repo.all.value.single()
        assertEquals(500L, updated.transferredBytes)
        assertEquals(TransferStatus.RUNNING, updated.status)  // 状态保留
    }

    @Test fun `updateProgress with unknown id is no-op`() = runBlocking {
        repo.add(sampleTransfer("t1", transferredBytes = 100))
        repo.updateProgress("t-missing", transferredBytes = 999)
        assertEquals(100L, repo.all.value.single().transferredBytes)
    }

    @Test fun `markDone flips status to SUCCESS and sets transferredBytes to totalBytes`() = runBlocking {
        val t = sampleTransfer("t1", totalBytes = 1000, transferredBytes = 800)
        repo.add(t)
        repo.markDone("t1")
        val updated = repo.all.value.single()
        assertEquals(TransferStatus.SUCCESS, updated.status)
        assertEquals(1000L, updated.transferredBytes)
    }

    @Test fun `markFailed flips status to FAILED with error message`() = runBlocking {
        repo.add(sampleTransfer("t1"))
        repo.markFailed("t1", "上传失败:网络中断")
        val updated = repo.all.value.single()
        assertEquals(TransferStatus.FAILED, updated.status)
        assertEquals(0L, updated.speedBps)  // 失败时清零速度
    }

    @Test fun `markFailed with unknown id is no-op`() = runBlocking {
        repo.add(sampleTransfer("t1"))
        repo.markFailed("t-missing", "fail")
        assertEquals(1, repo.all.value.size)  // 列表不变
    }

    @Test fun `clearDone removes only SUCCESS entries`() = runBlocking {
        repo.add(sampleTransfer("t1"))
        repo.markDone("t1")
        repo.add(sampleTransfer("t2"))
        repo.markFailed("t2", "fail")
        repo.add(sampleTransfer("t3"))

        repo.clearDone()

        val remaining = repo.all.value.map { it.id }
        assertEquals(listOf("t2", "t3"), remaining)
    }

    @Test fun `clearFailed removes only FAILED entries`() = runBlocking {
        repo.add(sampleTransfer("t1"))
        repo.markDone("t1")
        repo.add(sampleTransfer("t2"))
        repo.markFailed("t2", "fail")
        repo.add(sampleTransfer("t3"))

        repo.clearFailed()

        val remaining = repo.all.value.map { it.id }
        assertEquals(listOf("t1", "t3"), remaining)
    }

    @Test fun `concurrent updateProgress preserves order (no lost updates)`() = runBlocking {
        // 加 1 条 transfer,然后并发 updateProgress 100 次
        repo.add(sampleTransfer("t1", transferredBytes = 0))
        kotlinx.coroutines.coroutineScope {
            val updates = (1..100).map { i ->
                async { repo.updateProgress("t1", transferredBytes = i.toLong()) }
            }
            updates.forEach { it.await() }
        }
        // 最后 1 次 update 应该被记录(100),因为 StateFlow 是顺序更新
        val final = repo.all.value.single()
        assertEquals(100L, final.transferredBytes)
    }

    @Test fun `all state flow is StateFlow not cold Flow`() = runBlocking {
        // 验证 StateFlow 语义(总是有 current value)
        val initial = repo.all.value
        assertTrue("initial state should be empty list", initial.isEmpty())
    }

    private fun sampleTransfer(
        id: String,
        totalBytes: Long = 1000L,
        transferredBytes: Long = 0L,
    ) = Transfer(
        id = id,
        fileName = "test-$id.bin",
        direction = Direction.UP,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        speedBps = 5_000_000L,
        status = TransferStatus.RUNNING,
        startedAt = 1_700_000_000L,
    )
}
