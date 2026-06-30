package com.starvault.data.repository

import com.starvault.core.ServiceRateLimiter
import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.OpenFileDeleteResponse
import com.starvault.data.remote.cloud115.OpenFileMoveResponse
import com.starvault.data.remote.cloud115.OpenFileUpdateResponse
import com.starvault.data.remote.cloud115.OpenFolderAddData
import com.starvault.data.remote.cloud115.OpenFolderAddResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * FilesRepository 单元测试 — 聚焦 createFolder / deleteFiles / moveFiles / renameFile。
 * listFolder / searchFiles 走真实 JsonElement 解析,主要靠 Paparazzi / AVD 端到端回归。
 *
 * createFolder 测试重点:
 *  - state=true + data 非空 → Result.success + 透传 fileName/fileId
 *  - state=false + message 非空 → Result.failure(IllegalStateException,带 message)
 *  - HTTP 4xx/5xx → Result.failure(由 requireSuccessful 抛 IllegalStateException("HTTP ..."))
 *  - state=true 但 data=null → Result.failure(IllegalStateException("响应为空"))
 *  - pid 与 file_name 透传给 Retrofit @Field,Retrofit 自身 URL encode form field
 *
 * deleteFiles / moveFiles / renameFile 测试重点:
 *  - 业务成功 → Result.success(Unit),不再向下解析 data(delete/move 响应无 data)
 *  - state=false + message → Result.failure 带 115 端 message
 *  - HTTP 4xx/5xx → Result.failure 带 "HTTP xxx"
 *  - deleteFiles 空列表 → 短路不调 api,直接 Result.success
 *  - renameFile 只传 file_id + file_name(**不传 star**,避免误清星标 — 见 OpenFileApiService.updateFile 注释)
 *  - deleteFiles / moveFiles 的 file_ids 字段是逗号分隔字符串(对齐 OpenList strings.Join 行为)
 */
class FilesRepositoryTest {

    private val api = mockk<OpenFileApiService>()

    // 高 permitsPerSecond 让限速器 intervalMs ≈ 1ms,测试不真限速(避免 runTest 内部时序问题)
    private val noopLimiter = ServiceRateLimiter(permitsPerSecond = 1000.0)

    private val repo = FilesRepository(api, noopLimiter)

    @Test
    fun `createFolder success returns data`() = runTest {
        coEvery {
            api.addFolder(pid = "0", fileName = "新文件夹")
        } returns Response.success(
            OpenFolderAddResponse(
                state = true,
                message = "ok",
                code = 0,
                data = OpenFolderAddData(fileName = "新文件夹", fileId = "12345"),
            )
        )

        val result = repo.createFolder(name = "新文件夹", pid = "0")

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("新文件夹", data.fileName)
        assertEquals("12345", data.fileId)
        coVerify(exactly = 1) { api.addFolder(pid = "0", fileName = "新文件夹") }
    }

    @Test
    fun `createFolder state false returns failure with 115 message`() = runTest {
        coEvery {
            api.addFolder(pid = "0", fileName = any())
        } returns Response.success(
            OpenFolderAddResponse(
                state = false,
                message = "同层目录已存在同名文件夹",
                code = 990002,
                data = null,
            )
        )

        val result = repo.createFolder(name = "冲突", pid = "0")

        assertTrue(result.isFailure)
        assertEquals("同层目录已存在同名文件夹", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createFolder state true with null data returns failure`() = runTest {
        coEvery {
            api.addFolder(pid = "0", fileName = any())
        } returns Response.success(
            OpenFolderAddResponse(state = true, message = "", code = 0, data = null)
        )

        val result = repo.createFolder(name = "空 data", pid = "0")

        assertTrue(result.isFailure)
        assertEquals("响应为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createFolder HTTP 500 returns failure with status code`() = runTest {
        coEvery {
            api.addFolder(pid = any(), fileName = any())
        } returns Response.error(500, "".toResponseBody(null))

        val result = repo.createFolder(name = "x", pid = "0")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 500"))
    }

    @Test
    fun `createFolder propagates pid to underlying call`() = runTest {
        coEvery {
            api.addFolder(pid = "42", fileName = "子")
        } returns Response.success(
            OpenFolderAddResponse(state = true, message = "", code = 0,
                data = OpenFolderAddData(fileName = "子", fileId = "999"))
        )

        repo.createFolder(name = "子", pid = "42")

        coVerify(exactly = 1) { api.addFolder(pid = "42", fileName = "子") }
    }

    // ─────────────────── deleteFiles ───────────────────

    @Test
    fun `deleteFiles success returns Unit`() = runTest {
        coEvery {
            api.deleteFiles(fileIds = "1,2,3")
        } returns Response.success(
            OpenFileDeleteResponse(state = true, message = "", code = 0)
        )

        val result = repo.deleteFiles(listOf("1", "2", "3"))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.deleteFiles(fileIds = "1,2,3") }
    }

    @Test
    fun `deleteFiles joins ids with comma`() = runTest {
        coEvery {
            api.deleteFiles(fileIds = any())
        } returns Response.success(
            OpenFileDeleteResponse(state = true, message = "", code = 0)
        )

        repo.deleteFiles(listOf("100", "200", "300", "400"))

        // 验证逗号分隔,顺序与输入一致
        coVerify(exactly = 1) { api.deleteFiles(fileIds = "100,200,300,400") }
    }

    @Test
    fun `deleteFiles state false returns failure with 115 message`() = runTest {
        coEvery {
            api.deleteFiles(fileIds = any())
        } returns Response.success(
            OpenFileDeleteResponse(
                state = false,
                message = "文件不存在或已删除",
                code = 990003,
            )
        )

        val result = repo.deleteFiles(listOf("999"))

        assertTrue(result.isFailure)
        assertEquals("文件不存在或已删除", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteFiles HTTP 503 returns failure with status code`() = runTest {
        coEvery {
            api.deleteFiles(fileIds = any())
        } returns Response.error(503, "".toResponseBody(null))

        val result = repo.deleteFiles(listOf("1"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 503"))
    }

    @Test
    fun `deleteFiles empty list short-circuits without calling api`() = runTest {
        // 没 mock api.deleteFiles — 如果被调到,coVerify 会失败
        val result = repo.deleteFiles(emptyList())

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.deleteFiles(fileIds = any()) }
    }

    // ─────────────────── moveFiles ───────────────────

    @Test
    fun `moveFiles success returns Unit`() = runTest {
        coEvery {
            api.moveFiles(fileIds = "10,11", toCid = "42")
        } returns Response.success(
            OpenFileMoveResponse(state = true, message = "", code = 0)
        )

        val result = repo.moveFiles(listOf("10", "11"), toCid = "42")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.moveFiles(fileIds = "10,11", toCid = "42") }
    }

    @Test
    fun `moveFiles propagates toCid to underlying call`() = runTest {
        coEvery {
            api.moveFiles(fileIds = any(), toCid = any())
        } returns Response.success(
            OpenFileMoveResponse(state = true, message = "", code = 0)
        )

        repo.moveFiles(listOf("5"), toCid = "99")

        coVerify(exactly = 1) { api.moveFiles(fileIds = "5", toCid = "99") }
    }

    @Test
    fun `moveFiles state false returns failure with 115 message`() = runTest {
        coEvery {
            api.moveFiles(fileIds = any(), toCid = any())
        } returns Response.success(
            OpenFileMoveResponse(
                state = false,
                message = "目标目录不存在",
                code = 990001,
            )
        )

        val result = repo.moveFiles(listOf("1"), toCid = "0")

        assertTrue(result.isFailure)
        assertEquals("目标目录不存在", result.exceptionOrNull()?.message)
    }

    @Test
    fun `moveFiles HTTP 401 returns failure with status code`() = runTest {
        coEvery {
            api.moveFiles(fileIds = any(), toCid = any())
        } returns Response.error(401, "".toResponseBody(null))

        val result = repo.moveFiles(listOf("1"), toCid = "0")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 401"))
    }

    @Test
    fun `moveFiles empty list short-circuits without calling api`() = runTest {
        val result = repo.moveFiles(emptyList(), toCid = "0")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.moveFiles(fileIds = any(), toCid = any()) }
    }

    // ─────────────────── renameFile ───────────────────

    @Test
    fun `renameFile success returns Unit`() = runTest {
        coEvery {
            api.updateFile(fileId = "5", fileName = "新名.jpg")
        } returns Response.success(
            OpenFileUpdateResponse(state = true, message = "", code = 0)
        )

        val result = repo.renameFile(id = "5", newName = "新名.jpg")

        assertTrue(result.isSuccess)
        // 关键断言:star 字段**不能**被传(否则误清星标)
        coVerify(exactly = 1) { api.updateFile(fileId = "5", fileName = "新名.jpg", star = null) }
    }

    @Test
    fun `renameFile state false returns failure with 115 message`() = runTest {
        coEvery {
            api.updateFile(fileId = any(), fileName = any())
        } returns Response.success(
            OpenFileUpdateResponse(
                state = false,
                message = "文件名包含非法字符",
                code = 990004,
            )
        )

        val result = repo.renameFile(id = "5", newName = "../bad")

        assertTrue(result.isFailure)
        assertEquals("文件名包含非法字符", result.exceptionOrNull()?.message)
    }

    @Test
    fun `renameFile HTTP 500 returns failure with status code`() = runTest {
        coEvery {
            api.updateFile(fileId = any(), fileName = any())
        } returns Response.error(500, "".toResponseBody(null))

        val result = repo.renameFile(id = "5", newName = "x")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 500"))
    }
}
