package com.starvault.ui.files

import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.PagedFiles
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FilesViewModel 测试 — 聚焦本地态（不调真接口）。
 *
 *  覆盖：
 *  - selectType 切换 activeType 并重算 totalCount（筛后数量，不含文件夹）
 *  - toggleSelect 增删 selectedIds
 *  - clearSelection 清空
 *  - setFolder 相同 cid noop，不同 cid 才重新调 api
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // VM 失败时调 ToastBus.error;stub 避免走真 Channel
        mockkObject(ToastBus)
        every { ToastBus.error(any()) } returns Unit
        // ServiceLocator.filesRefreshTrigger 是进程级 Channel(单 collector);前面 test 的
        // VM collector 还活着,新 emit 的 Unit 会被旧 collector 抢走。setUp 阶段 reset
        // 整个 channel,确保每个 test 独立。
        com.starvault.core.ServiceLocator.resetFilesRefreshTriggerForTest()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ToastBus)
    }

    private fun folder(id: String, name: String) = ParsedFileItem(
        id = id, parentId = "0", name = name, pickCode = "",
        isFolder = true,
    )
    private fun video(id: String, name: String) = ParsedFileItem(
        id = id, parentId = "0", name = name, ico = "mp4", sizeBytes = 1_000_000,
        mtimeSec = 0, pickCode = "", isFolder = false, playLong = 60, sha1 = "", fileCategory = 2,
    )
    private fun audio(id: String, name: String) = ParsedFileItem(
        id = id, parentId = "0", name = name, ico = "mp3", sizeBytes = 500_000,
        mtimeSec = 0, pickCode = "", isFolder = false, playLong = 180, sha1 = "", fileCategory = 1,
    )

    @Test
    fun `selectType filters totalCount to non-folder matching types`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = listOf(folder("f1", "手机相册"), video("v1", "a.mp4"), audio("a1", "b.mp3")),
                       offset = 0, limit = 50, totalCount = 3, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        // 等 init loadJob 完成（UnconfinedTestDispatcher 直接同步推进）
        testScheduler.advanceUntilIdle()

        val s = vm.state.value as FilesUiState.Success
        assertEquals(3, s.totalCount)          // 全部 = 3 (含文件夹)
        assertEquals(3, s.tabCounts.all)       // all 含文件夹
        assertEquals(1, s.tabCounts.video)
        assertEquals(1, s.tabCounts.audio)
        assertEquals(0, s.tabCounts.image)

        // 切到 AUDIO：totalCount 只数匹配的（不含文件夹）
        vm.selectType(FileType.AUDIO)
        val filtered = vm.state.value as FilesUiState.Success
        assertEquals(FileType.AUDIO, filtered.activeType)
        assertEquals(1, filtered.totalCount)

        // 再切回 null（全部）
        vm.selectType(null)
        val all = vm.state.value as FilesUiState.Success
        assertNull(all.activeType)
        assertEquals(3, all.totalCount)
    }

    @Test
    fun `toggleSelect adds then removes id`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = listOf(folder("f1", "手机相册")),
                       offset = 0, limit = 50, totalCount = 1, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()

        vm.toggleSelect("f1")
        assertEquals(setOf("f1"), (vm.state.value as FilesUiState.Success).selectedIds)

        vm.toggleSelect("f1")
        assertTrue((vm.state.value as FilesUiState.Success).selectedIds.isEmpty())
    }

    @Test
    fun `clearSelection empties set`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = listOf(folder("f1", "x"), folder("f2", "y")),
                       offset = 0, limit = 50, totalCount = 2, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()

        vm.toggleSelect("f1")
        vm.toggleSelect("f2")
        assertEquals(2, (vm.state.value as FilesUiState.Success).selectedIds.size)

        vm.clearSelection()
        assertTrue((vm.state.value as FilesUiState.Success).selectedIds.isEmpty())
    }

    @Test
    fun `setFolder with different cid triggers repo refetch`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = emptyList(), offset = 0, limit = 50, totalCount = 0, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()
        // init 已调过一次 listFolder("0")
        coVerify(exactly = 1) { repo.listFolder("0") }

        vm.setFolder("123")
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repo.listFolder("123") }
    }

    @Test
    fun `setFolder with same cid when already in Success is no-op`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = listOf(folder("f1", "x")),
                       offset = 0, limit = 50, totalCount = 1, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repo.listFolder("0") }

        // 当前已是 Success(cid="0")，再 setFolder("0") 应被短路
        vm.setFolder("0")
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repo.listFolder("0") }   // 没新增调用
    }

    @Test
    fun `refresh re-fetches current cid`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = emptyList(), offset = 0, limit = 50, totalCount = 0, hasMore = false)
        )
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repo.listFolder("0") }

        vm.refresh()
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 2) { repo.listFolder("0") }
    }

    @Test
    fun `repo failure keeps Success state untouched and toasts`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.failure(RuntimeException("network down"))
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()

        // 失败时 _state 不动(init 时 VM 先 set Loading → set Success(空列表) → 失败保留 Success 不动)
        // 屏不显示 Error 占位,看到的是空列表(无错误提示)
        val s = vm.state.value
        assertTrue("expected Success, got $s", s is FilesUiState.Success)
        assertTrue("expected empty list, got ${(s as FilesUiState.Success).all.size}", s.all.isEmpty())
        // 错误通过 ToastBus.error 提示
        verify(exactly = 1) { ToastBus.error("network down") }
    }

    /**
     * 回归测试 — Files 刷新 trigger 在 no-collector 窗口 emit,VM 起来后仍应消费。
     *
     * 复现 race:用户上传完成后已离开 Files 屏,TransfersVM 触发 `filesRefreshTrigger.trySend`
     * 时无 collector → 旧 SharedFlow(replay=0) 直接丢信号;新 Channel(UNLIMITED) buffer 起来。
     *
     * 用户切回 Files,新 FilesVM init 时 subscribe → drain buffer → 调 refresh() →
     * listFolder 被调第二次(首次来自 init 主动 loadFolder,第二次来自 trigger 触发 refresh)。
     *
     * 跑本 test 在旧的 SharedFlow 实现上**会失败**(只调 1 次 listFolder,signal 被丢);
     * 新 Channel 实现通过。
     */
    @Test
    fun `refresh trigger queued before VM construction causes refresh after init`() = runTest {
        // 模拟:TransfersVM 在 FilesVM 还没创建时先 trySend(VM 离开 Files 期间发生)
        // Channel(UNLIMITED) buffer 起来,不丢。
        com.starvault.core.ServiceLocator.filesRefreshTrigger.trySend(Unit)

        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.success(
            PagedFiles(items = emptyList(), offset = 0, limit = 50, totalCount = 0, hasMore = false)
        )
        FilesViewModel(repo)
        // init 同步发射:loadJob.launch(loadFolder cid=0) + collect.subscribe(drain Channel buffer → refresh → loadJob.cancel + relaunch)
        // UnconfinedTestDispatcher 上 init 返回时这些 launch 全部已入队,需要 advanceUntilIdle 让 listFolder 两次都跑完
        testScheduler.advanceUntilIdle()

        // init 主动 loadFolder("0") 1 次 + trigger 触发 refresh 1 次 = 2 次
        coVerify(atLeast = 2) { repo.listFolder(any()) }
    }
}