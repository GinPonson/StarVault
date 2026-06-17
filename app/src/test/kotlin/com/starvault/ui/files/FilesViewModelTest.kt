package com.starvault.ui.files

import com.starvault.core.ServiceLocator
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            listOf(folder("f1", "手机相册"), video("v1", "a.mp4"), audio("a1", "b.mp3"))
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
        coEvery { repo.listFolder(any()) } returns Result.success(listOf(folder("f1", "手机相册")))
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
        coEvery { repo.listFolder(any()) } returns Result.success(listOf(folder("f1", "x"), folder("f2", "y")))
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
        coEvery { repo.listFolder(any()) } returns Result.success(emptyList())
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
        coEvery { repo.listFolder(any()) } returns Result.success(listOf(folder("f1", "x")))
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
        coEvery { repo.listFolder(any()) } returns Result.success(emptyList())
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repo.listFolder("0") }

        vm.refresh()
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 2) { repo.listFolder("0") }
    }

    @Test
    fun `repo failure emits Error state`() = runTest {
        val repo = mockk<FilesRepository>()
        coEvery { repo.listFolder(any()) } returns Result.failure(RuntimeException("network down"))
        val vm = FilesViewModel(repo)
        testScheduler.advanceUntilIdle()

        val s = vm.state.value
        assertTrue("expected Error, got $s", s is FilesUiState.Error)
        assertEquals("network down", (s as FilesUiState.Error).message)
    }
}