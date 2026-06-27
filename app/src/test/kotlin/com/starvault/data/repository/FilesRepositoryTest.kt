package com.starvault.data.repository

import com.starvault.data.remote.cloud115.OpenFileApiService
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
 * FilesRepository 单元测试 — 聚焦 createFolder(其他 listFolder / searchFiles
 * 走真实 JsonElement 解析,主要靠 Paparazzi / AVD 端到端回归)。
 *
 * createFolder 测试重点:
 *  - state=true + data 非空 → Result.success + 透传 fileName/fileId
 *  - state=false + message 非空 → Result.failure(IllegalStateException,带 message)
 *  - HTTP 4xx/5xx → Result.failure(由 requireSuccessful 抛 IllegalStateException("HTTP ..."))
 *  - state=true 但 data=null → Result.failure(IllegalStateException("响应为空"))
 *  - pid 与 file_name 透传给 Retrofit @Field,Retrofit 自身 URL encode form field
 */
class FilesRepositoryTest {

    private val api = mockk<OpenFileApiService>()
    private val repo = FilesRepository(api)

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
}
