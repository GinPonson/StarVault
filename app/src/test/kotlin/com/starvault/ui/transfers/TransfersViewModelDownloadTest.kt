package com.starvault.ui.transfers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.starvault.core.ServiceLocator
import com.starvault.data.downloadworker.DownloadWork
import com.starvault.data.model.Direction
import com.starvault.data.model.TransferStatus
import com.starvault.data.repository.TransferRepository
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * TransfersViewModel.observeDownloadWork 测试 — 验证 envelope 桥接契约:
 *  - 入口立即建 RUNNING 占位 entry(Direction.DOWN,fileName/sizeBytes 来自 envelope)
 *  - 多并发观察(2 个 envelope)各自建独立 entry,互不覆盖
 *
 * WorkInfo 流转(Phase DONE → markDone / FAILED → markFailed)依赖真实 Worker 跑通,
 * 由 AVD 端到端覆盖;这里只验证 envelope → 占位的契约层。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransfersViewModelDownloadTest {

    private val repo = TransferRepository()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 替换 ServiceLocator.appScope / appContext / downloadWorkFlow 为 test-controlled 实例
        // (observeDownloadWork 内部 collect 跑在 appScope;init 订阅 downloadWorkFlow)
        mockkObject(ServiceLocator)
        every { ServiceLocator.appScope } returns CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate,
        )
        every { ServiceLocator.appContext } returns context
        val downloadChannel = Channel<DownloadWork>(Channel.UNLIMITED)
        every { ServiceLocator.downloadWorkFlow } returns downloadChannel.receiveAsFlow()

        // 初始化 Test WorkManager — observeDownloadWork 内 getInstance(context) 不抛
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().build(),
        )
        assertNotNull(WorkManager.getInstance(context))
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ServiceLocator)
    }

    private fun envelope(
        workId: UUID = UUID.randomUUID(),
        fileName: String = "song.flac",
        sizeBytes: Long = 5_242_880L,
    ) = DownloadWork(workId = workId, fileName = fileName, sizeBytes = sizeBytes)

    private fun vm() = TransfersViewModel(repo = repo)

    @Test fun `observeDownloadWork adds RUNNING placeholder with envelope metadata`() = runTest {
        val v = vm()
        val workId = UUID.randomUUID()
        v.observeDownloadWork(envelope(workId = workId, fileName = "movie.mp4", sizeBytes = 100_000_000L))

        // 占位 entry 立即可见(envelope 直接调 repo.add,不依赖 WorkInfo 首帧)
        val entry = repo.all.value.find { it.id == workId.toString() }
        assertNotNull("placeholder entry should exist for workId", entry)
        assertEquals("movie.mp4", entry!!.fileName)
        assertEquals(100_000_000L, entry.totalBytes)
        assertEquals(0L, entry.transferredBytes)
        assertEquals(Direction.DOWN, entry.direction)
        assertEquals(TransferStatus.RUNNING, entry.status)
    }

    @Test fun `observeDownloadWork handles multiple envelopes independently`() = runTest {
        val v = vm()
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        v.observeDownloadWork(envelope(workId = id1, fileName = "a.zip", sizeBytes = 1_000L))
        v.observeDownloadWork(envelope(workId = id2, fileName = "b.zip", sizeBytes = 2_000L))

        // 2 个独立 entry 都建好
        assertEquals(2, repo.all.value.size)
        val e1 = repo.all.value.find { it.id == id1.toString() }!!
        val e2 = repo.all.value.find { it.id == id2.toString() }!!
        assertEquals("a.zip", e1.fileName)
        assertEquals(1_000L, e1.totalBytes)
        assertEquals("b.zip", e2.fileName)
        assertEquals(2_000L, e2.totalBytes)
        assertTrue(e1.direction == Direction.DOWN)
        assertTrue(e2.direction == Direction.DOWN)
    }
}