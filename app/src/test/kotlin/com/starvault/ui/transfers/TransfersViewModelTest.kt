package com.starvault.ui.transfers

import com.starvault.core.ServiceLocator
import com.starvault.data.model.Direction
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.data.repository.TransferRepository
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TransfersViewModel 单元测试 — 验证 [TransfersViewModel] 状态聚合逻辑。
 *
 * 关键约束:
 *  - 注入 TransferRepository(fake / 真实)→ VM state 反映 repo 状态
 *  - 切换 tab 持久
 *  - pauseAll / togglePause / clearDone / retry 改写 repo 内容
 *  - totalActive / totalDone / totalOffline / upSpeedBps / downSpeedBps 跟 state 同步
 *
 * 真实 WorkInfo observe 在 [UploadWorker] Robolectric 集成测试中验证(不在这)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransfersViewModelTest {

    private val repo = TransferRepository()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // VM 内部不直接调 ToastBus,但 mockkObject 保险
        mockkObject(ServiceLocator)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ServiceLocator)
    }

    private fun vm() = TransfersViewModel(repo = repo)

    @Test fun `initial state is Success with empty list`() {
        val v = vm()
        // UnconfinedTestDispatcher 让 stateIn 立即同步 emit Success(empty)
        assertTrue("expected Success state, got: ${v.state.value}",
            v.state.value is TransfersUiState.Success)
        assertTrue((v.state.value as TransfersUiState.Success).all.isEmpty())
    }

    @Test fun `state reflects repo add and markDone`() = runTest {
        val v = vm()
        val t1 = sampleTransfer("t1", direction = Direction.UP, totalBytes = 1000, transferredBytes = 500, status = TransferStatus.RUNNING)
        repo.add(t1)
        // UnconfinedTestDispatcher 让 combine 同步 push
        val s = v.state.value as TransfersUiState.Success
        assertEquals(1, s.all.size)
        assertEquals(1, s.totalActive)

        repo.markDone("t1")
        val s2 = v.state.value as TransfersUiState.Success
        assertEquals(1, s2.totalDone)
        assertEquals(0, s2.totalActive)
    }

    @Test fun `selectTab updates activeTab`() {
        val v = vm()
        v.selectTab(TransfersTab.Done)
        assertEquals(TransfersTab.Done, (v.state.value as TransfersUiState.Success).activeTab)
    }

    @Test fun `clearDone removes SUCCESS entries from state`() = runTest {
        val v = vm()
        repo.add(sampleTransfer("t1"))
        repo.markDone("t1")
        repo.add(sampleTransfer("t2"))
        assertEquals(2, (v.state.value as TransfersUiState.Success).all.size)

        v.clearDone()
        val remaining = (v.state.value as TransfersUiState.Success).all
        assertEquals(1, remaining.size)
        assertEquals("t2", remaining[0].id)
    }

    @Test fun `pauseAll flips RUNNING to PAUSED in repo`() = runTest {
        val v = vm()
        repo.add(sampleTransfer("t1", status = TransferStatus.RUNNING))
        repo.add(sampleTransfer("t2", status = TransferStatus.RUNNING))

        v.pauseAll()
        val s = v.state.value as TransfersUiState.Success
        assertTrue(s.all.all { it.status == TransferStatus.PAUSED })
    }

    @Test fun `retry flips FAILED to RUNNING in repo`() = runTest {
        val v = vm()
        repo.add(sampleTransfer("t1", status = TransferStatus.FAILED))

        v.retry(repo.all.value.single())
        val s = v.state.value as TransfersUiState.Success
        assertEquals(TransferStatus.RUNNING, s.all.single().status)
    }

    @Test fun `up and down speedBps aggregate from RUNNING entries`() = runTest {
        val v = vm()
        repo.add(sampleTransfer("u1", direction = Direction.UP, speedBps = 1_000_000L, status = TransferStatus.RUNNING))
        repo.add(sampleTransfer("u2", direction = Direction.UP, speedBps = 2_000_000L, status = TransferStatus.RUNNING))
        repo.add(sampleTransfer("d1", direction = Direction.DOWN, speedBps = 3_000_000L, status = TransferStatus.RUNNING))
        repo.add(sampleTransfer("d2", direction = Direction.DOWN, speedBps = 4_000_000L, status = TransferStatus.PAUSED))  // PAUSED 不计
        val s = v.state.value as TransfersUiState.Success
        assertEquals(3_000_000L, s.upSpeedBps)
        assertEquals(3_000_000L, s.downSpeedBps)
    }

    @Test fun `togglePause flips RUNNING to PAUSED and back`() = runTest {
        val v = vm()
        repo.add(sampleTransfer("t1", status = TransferStatus.RUNNING))

        v.togglePause(repo.all.value.single())
        assertEquals(TransferStatus.PAUSED, repo.all.value.single().status)

        v.togglePause(repo.all.value.single())
        assertEquals(TransferStatus.RUNNING, repo.all.value.single().status)
    }

    private fun sampleTransfer(
        id: String,
        direction: Direction = Direction.UP,
        totalBytes: Long = 1000L,
        transferredBytes: Long = 0L,
        speedBps: Long = 1_000_000L,
        status: TransferStatus = TransferStatus.RUNNING,
    ) = Transfer(
        id = id,
        fileName = "$id.bin",
        direction = direction,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        speedBps = speedBps,
        status = status,
        startedAt = 1_700_000_000L,
    )
}
