package com.starvault.data.repository

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.starvault.data.remote.cloud115.ParsedFileItem
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.channels.Channel
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
 * DownloadRepository 单元测试 — 覆盖:
 *  - 文件夹 → Result.failure("文件夹不支持下载")
 *  - 缺 pickCode → Result.failure("缺少提取码")
 *  - 合法文件 → DownloadWorker.enqueue + downloadWorkTrigger.trySend + Result.success(UUID)
 *  - 失败路径不桥接到 channel
 *
 * 用 WorkManagerTestInitHelper 初始化 WorkManager(项目已引 work-testing) —
 * 不需要 Configuration.Provider Application,Robolectric 即可跑真实 WorkManager.enqueue。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val channel = Channel<UUID>(Channel.UNLIMITED)
    private val repo = DownloadRepository(context = context, downloadWorkTrigger = channel)

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        // 初始化 Robolectric 用的 WorkManager(同步 executor,测试不真起后台)
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.WARN)
                .build(),
        )
    }

    @After fun teardown() {
        unmockkStatic(Log::class)
    }

    @Test fun `enqueue returns failure when item is folder`() {
        val folder = ParsedFileItem(
            id = "cid_123",
            parentId = "cid_0",
            name = "Documents",
            isFolder = true,
            pickCode = "abc",
        )

        val result = repo.enqueue(folder)

        assertTrue("expected failure for folder", result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertEquals("文件夹不支持下载", ex!!.message)
        // 失败路径不应投递 WorkRequest → channel 无信号
        assertEquals(null, drainOneOrNull())
    }

    @Test fun `enqueue returns failure when pickCode is blank`() {
        val noPickCode = ParsedFileItem(
            id = "fid_456",
            parentId = "cid_0",
            name = "secret.bin",
            isFolder = false,
            pickCode = "",  // 空 → 校验失败
            sizeBytes = 1024L,
        )

        val result = repo.enqueue(noPickCode)

        assertTrue("expected failure for blank pickCode", result.isFailure)
        assertEquals("缺少提取码", result.exceptionOrNull()!!.message)
        assertEquals(null, drainOneOrNull())
    }

    @Test fun `enqueue returns success and triggers channel when item is valid file`() {
        val file = ParsedFileItem(
            id = "fid_789",
            parentId = "cid_0",
            name = "song.flac",
            isFolder = false,
            pickCode = "pickcode_xyz",
            sizeBytes = 5_242_880L,
        )

        val result = repo.enqueue(file)

        assertTrue("expected success for valid file", result.isSuccess)
        val workId = result.getOrNull()
        assertNotNull("workId should be returned", workId)

        // 桥接:channel 应收到 1 条 workId(先验 channel,再验 WorkManager 用 channel 拿到的 id)
        val receivedUuid = drainOne()
        assertEquals(workId, receivedUuid)

        // 真实 WorkManager:workId 对应的 WorkInfo 应存在
        val info = WorkManager.getInstance(context).getWorkInfoById(workId!!).get()
        assertNotNull("WorkInfo should exist for enqueued workId", info)
    }

    @Test fun `enqueue failure path does not bridge to channel`() {
        // 校验失败时不应往 channel 发任何信号(避免 TransfersViewModel 收到空 UUID 触发空 observe)
        val folder = ParsedFileItem(
            id = "cid_x",
            parentId = "cid_0",
            name = "Music",
            isFolder = true,
            pickCode = "any",
        )

        repo.enqueue(folder)

        // tryReceive 在空 channel 上返回 failure ChannelResult → getOrNull() == null
        val receivedUuid = drainOneOrNull()
        assertEquals(null, receivedUuid)
    }

    /** 取出 channel 中 1 个 workId(假设 channel 至少 1 条),并返回它。 */
    private fun drainOne(): UUID {
        val r = channel.tryReceive()
        val v = r.getOrNull()
        assertNotNull("channel should have a UUID to drain", v)
        return v!!
    }

    /** 取出 channel 中 1 个 workId(允许空) — 空时返回 null。 */
    private fun drainOneOrNull(): UUID? = channel.tryReceive().getOrNull()
}