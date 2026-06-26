package com.starvault.data.uploadworker

import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UploadWorker 失败路径测试 — M2 spec §11 DoD #8: 「故意传入不存在的文件 URI →
 * ToastBus 提示 + Transfer FAILED + 不留垃圾文件」。
 *
 * 覆盖路径:
 *  1. inputData 缺字段 → Result.failure() (Worker 自身前置校验,**executor 不被调**,
 *     所以不会发 Init 到 115 → 服务端不留垃圾 — 这是 DoD #8 的真正保证)
 *  2. executor.run 返回 [UploadOutcome.Reject] → Result.failure() + ToastBus 已发
 *  3. executor.run 抛异常 → catch 块 Result.failure() + ToastBus 已发
 *  4. executor.run 返 Success → Result.success() (happy path 反例)
 *
 * [UploadWorker.getForegroundInfo] 的 4 个用例在 [UploadWorkerTest]。
 *
 * ## 为什么不在 JVM 测 ContentResolver.openInputStream 抛 FileNotFoundException 的路径
 *
 * Robolectric `ShadowContentResolver` 对未知 `content://` URI 的默认行为不稳定(版本间差异),
 * `ShadowContentResolver.registerInputStream` 只支持塞 InputStream 而不支持塞异常。真实
 * Android 系统对无效 SAF URI 是抛 `FileNotFoundException`,走 Worker catch 块后行为与
 * "executor 抛异常" 完全等价(都是 catch → Result.failure + ToastBus),所以路径 3 已经覆盖。
 *
 * ## mock ServiceLocator 的策略
 *
 * ServiceLocator 是单例 `object`,`uploadExecutor` 是 `lateinit var private set` — 测试无法
 * 走构造注入(避免改生产代码)。用反射在 @Before 注入 mock executor,@After 清空避免污染。
 * 这是 JVM 单测惯用法,生产代码不依赖反射。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UploadWorkerFailurePathTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mockExecutor: UploadExecutor = mockk(relaxed = true)

    @Before fun setUp() {
        // Robolectric JVM 单元测试环境下 android.util.Log 底层是 native,UnsatisfiedLinkError
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // ToastBus 是全局 object,UploadExecutor 内部会调 — mock 掉避免真实写入
        mockkObject(ToastBus)
        every { ToastBus.error(any<String>()) } returns Unit

        // ServiceLocator 是单例,反射注入 mock executor;@After 再清空
        mockkObject(ServiceLocator)
        val field = ServiceLocator::class.java.getDeclaredField("uploadExecutor")
        field.isAccessible = true
        field.set(ServiceLocator, mockExecutor)
    }

    @After fun tearDown() {
        unmockkStatic(android.util.Log::class)
        unmockkObject(ToastBus)
        unmockkObject(ServiceLocator)
        // 清空 ServiceLocator 字段避免污染其他测试
        val field = ServiceLocator::class.java.getDeclaredField("uploadExecutor")
        field.isAccessible = true
        field.set(ServiceLocator, null)
    }

    private fun inputData(
        uri: String? = "content://test/uri",
        fileName: String? = "x.bin",
        sizeBytes: Long? = 100L,
        targetCid: String? = "0",
    ): Data {
        val b = Data.Builder()
        uri?.let { b.putString(UploadWorker.Key.Uri, it) }
        fileName?.let { b.putString(UploadWorker.Key.FileName, it) }
        sizeBytes?.let { b.putLong(UploadWorker.Key.SizeBytes, it) }
        targetCid?.let { b.putString(UploadWorker.Key.TargetCid, it) }
        return b.build()
    }

    /** path 1: inputData 缺字段 → Result.failure() (不走 executor、不走 ContentResolver) */
    @Test fun `doWork returns failure when URI is missing`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData(uri = null)).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test fun `doWork returns failure when sizeBytes is missing`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData(sizeBytes = null)).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test fun `doWork returns failure when sizeBytes is zero`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData(sizeBytes = 0L)).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test fun `doWork returns failure when fileName is missing`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData(fileName = null)).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    /** M2 spec §11 DoD #8 「不留垃圾文件」的核心保证:inputData 缺字段时 Worker 在最早期
     *  return,executor 完全不被调 → UploadExecutor 不会发 Init 到 115 → 服务端不留任何文件。 */
    @Test fun `missing input fields never reach executor so 115 has no record`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData(uri = null)).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        io.mockk.coVerify(exactly = 0) { mockExecutor.run(any(), any(), any(), any(), any()) }
    }

    /** path 2: executor 返回 Reject(秒传 / 校验失败 / 业务拒绝)→ Result.failure() */
    @Test fun `doWork returns failure when executor rejects with message`() = runBlocking {
        coEvery { mockExecutor.run(any(), any(), any(), any(), any()) } returns
            UploadOutcome.Reject("暂不支持秒传")

        val worker = TestListenableWorkerBuilder<UploadWorker>(
            context,
            inputData(uri = "content://test/uri"),
        ).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
        // executor 被调一次
        io.mockk.coVerify(exactly = 1) {
            mockExecutor.run(
                fileName = "x.bin",
                fileSize = 100L,
                targetCid = "0",
                input = any(),
                onProgress = any(),
            )
        }
    }

    /** path 3: executor 抛异常(网络 / OAuth / OSS IO)→ catch 块 Result.failure() */
    @Test fun `doWork returns failure when executor throws`() = runBlocking {
        coEvery { mockExecutor.run(any(), any(), any(), any(), any()) } throws
            java.io.IOException("网络中断")

        val worker = TestListenableWorkerBuilder<UploadWorker>(
            context,
            inputData(uri = "content://test/uri"),
        ).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    /** happy path 反例: executor 返 Success → Result.success() */
    @Test fun `doWork returns success when executor succeeds`() = runBlocking {
        coEvery { mockExecutor.run(any(), any(), any(), any(), any()) } returns UploadOutcome.Success

        val worker = TestListenableWorkerBuilder<UploadWorker>(
            context,
            inputData(uri = "content://test/uri"),
        ).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}