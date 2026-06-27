package com.starvault.ui.files

import com.starvault.core.ToastBus
import com.starvault.data.repository.DownloadRepository
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.PagedFiles
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * FilesViewModel.downloadEntry 测试 — 覆盖 3 条路径:
 *  - 文件夹 → ToastBus.error("文件夹不支持下载")
 *  - 缺 pickCode → ToastBus.error("缺少提取码")
 *  - 合法文件 → DownloadRepository.enqueue + ToastBus.info("开始下载：xxx")
 *  - repo 抛异常 → ToastBus.error(exception.message)
 *
 * FilesViewModel 构造默认从 ServiceLocator.filesRepository 拉根目录,所以本测试必须
 * 显式注入 mock filesRepo(filesRepository 参数),否则 init 触发 ServiceLocator 访问
 * → UninitializedPropertyAccessException。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelDownloadTest {

    private val filesRepo = mockk<FilesRepository>()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(ToastBus)
        every { ToastBus.error(any()) } returns Unit
        every { ToastBus.info(any()) } returns Unit
        // 让 init loadJob("0") 不抛 — 返回空 PagedFiles,VM 立刻进 Success(empty)
        coEvery { filesRepo.listFolder(any()) } returns Result.success(
            PagedFiles(items = emptyList(), offset = 0, limit = 50, totalCount = 0, hasMore = false),
        )
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ToastBus)
    }

    private fun fileEntry(
        isFolder: Boolean = false,
        pickCode: String = "pick_xyz",
        name: String = "song.flac",
        sizeBytes: Long = 5_242_880L,
        id: String = "fid_1",
    ) = FileEntry(
        id = id,
        name = name,
        type = com.starvault.data.model.FileType.OTHER,
        metaSegments = listOf("5 MB"),
        isFolder = isFolder,
        thumbnailUrl = null,
        pickCode = pickCode,
        sizeBytes = sizeBytes,
    )

    private fun vm(downloadRepo: DownloadRepository): FilesViewModel =
        FilesViewModel(filesRepository = filesRepo, downloadRepository = downloadRepo)

    @Test fun `downloadEntry with folder shows error and does not toast info`() {
        val repo = mockk<DownloadRepository>()
        every { repo.enqueue(any()) } returns Result.failure(IllegalStateException("文件夹不支持下载"))
        val v = vm(repo)
        val entry = fileEntry(isFolder = true)

        v.downloadEntry(entry)

        verify(exactly = 1) { repo.enqueue(any()) }
        verify(exactly = 1) { ToastBus.error("文件夹不支持下载") }
        verify(exactly = 0) { ToastBus.info(any()) }
    }

    @Test fun `downloadEntry with blank pickCode shows error and does not toast info`() {
        val repo = mockk<DownloadRepository>()
        every { repo.enqueue(any()) } returns Result.failure(IllegalStateException("缺少提取码"))
        val v = vm(repo)
        val entry = fileEntry(pickCode = "")

        v.downloadEntry(entry)

        verify(exactly = 1) { repo.enqueue(any()) }
        verify(exactly = 1) { ToastBus.error("缺少提取码") }
        verify(exactly = 0) { ToastBus.info(any()) }
    }

    @Test fun `downloadEntry with valid file enqueues and toasts info`() {
        val repo = mockk<DownloadRepository>()
        every { repo.enqueue(any()) } returns Result.success(UUID.randomUUID())
        val v = vm(repo)
        val entry = fileEntry(name = "song.flac", sizeBytes = 5_242_880L)

        v.downloadEntry(entry)

        verify(exactly = 1) { repo.enqueue(any()) }
        verify(exactly = 1) { ToastBus.info("开始下载：song.flac") }
        verify(exactly = 0) { ToastBus.error(any()) }
    }

    @Test fun `downloadEntry on repo failure toasts error message`() {
        // 校验通过但下游异常(罕见,例如 WorkManager 抛)
        val repo = mockk<DownloadRepository>()
        every { repo.enqueue(any()) } returns Result.failure(IllegalStateException("WorkManager 不可用"))
        val v = vm(repo)
        val entry = fileEntry()

        v.downloadEntry(entry)

        verify(exactly = 1) { ToastBus.error("WorkManager 不可用") }
        verify(exactly = 0) { ToastBus.info(any()) }
    }
}