package com.starvault.data.uploadworker

import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.CallbackInfo
import com.starvault.data.remote.cloud115.OpenUploadApiService
import com.starvault.data.remote.cloud115.UploadCallback
import com.starvault.data.remote.cloud115.UploadGetTokenResp
import com.starvault.data.remote.cloud115.UploadInitResp
import com.starvault.data.upload.OssUploader
import com.starvault.data.upload.Sha1Hashing
import com.starvault.data.upload.UploadInitClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * UploadExecutor 纯 JVM 单元测试 — 不依赖 WorkManager runtime / Robolectric。
 *
 * 把 Worker 主体编排(Init → Status 分支 → Sign check 回路 → GetToken → OssUpload)
 * 抽到 [UploadExecutor] 纯类后,5 个 plan case 都能在纯 JVM 跑:
 *  - (a) status=1 happy path → success
 *  - (b) status=2 → Reject + ToastBus("暂不支持秒传")
 *  - (c) status=6 → 1(reInit 成功)→ OssUploader.upload 1 次
 *  - (d) status=6 → 6(reInit 仍 verify)→ failure + ToastBus("文件校验失败")
 *  - (e) OssUploader.upload 抛 → failure + ToastBus
 *
 * UploadWorker 本身只负责 WorkManager 集成(setProgress/setForeground/URI→InputStream),
 * Phase 4 加 Robolectric 后再单测。
 */
class UploadExecutorTest {

    private val api: OpenUploadApiService = mockk(relaxed = false)
    private val initClient = UploadInitClient(api)
    private val ossUploader: OssUploader = mockk(relaxed = false)
    private val sha1: Sha1Hashing = mockk(relaxed = false)
    private val executor = UploadExecutor(
        uploadInitClient = initClient,
        ossUploader = ossUploader,
        api = api,
    )

    @Before fun setUp() {
        // mockkObject(ToastBus) — ToastBus 是全局 object
        mockkObject(ToastBus)
        every { ToastBus.error(any<String>()) } returns Unit
        every { ToastBus.info(any<String>()) } returns Unit
        // mockkStatic(android.util.Log) — UploadExecutor 内部 Log.w 调底层 native,
        // JVM 单元测试环境下 UnsatisfiedLinkError
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After fun tearDown() {
        unmockkObject(ToastBus)
        unmockkStatic(android.util.Log::class)
    }

    // ---------- (a) happy path ----------

    @Test fun `status 1 happy path calls ossUploader once and returns success`() = runBlocking {
        val initResp = UploadInitResp(
            status = 1, sign_key = "", sign_check = "", file_id = "F1",
            target = "U_1_0", bucket = "test-bucket", `object` = "test-object",
            callback = UploadCallback.Single(CallbackInfo("http://cb", "")),
            pick_code = "",
        )
        coEvery { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) } returns Response.success(initResp)
        coEvery { api.getUploadToken() } returns Response.success(
            UploadGetTokenResp(
                endpoint = "https://oss-cn-shanghai.aliyuncs.com",
                AccessKeyId = "AK", AccessKeySecret = "SK", SecurityToken = "STS",
                expiration = "2026-06-25T12:00:00Z",
            )
        )
        coEvery { ossUploader.upload(any(), any(), any(), any(), any(), any()) } returns Unit

        val input = ByteArrayInputStream(ByteArray(10 * 1024 * 1024))
        val result = executor.run(
            fileName = "a.bin",
            fileSize = 10L * 1024 * 1024,
            targetCid = "0",
            input = input,
            onProgress = { _, _ -> },
        )

        assertTrue("expected UploadOutcome.Success, got: $result", result is UploadOutcome.Success)
        coVerify(exactly = 1) { ossUploader.upload(any(), any(), any(), any(), any(), any()) }
    }

    // ---------- (b) 秒传 ----------

    @Test fun `status 2 returns Reject with toast 暂不支持秒传`() = runBlocking {
        val initResp = UploadInitResp(
            status = 2, sign_key = "", sign_check = "", file_id = "F2",
            target = "U_1_0", bucket = "b", `object` = "o",
            callback = UploadCallback.Single(CallbackInfo("", "")),
            pick_code = "",
        )
        coEvery { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) } returns Response.success(initResp)

        val result = executor.run(
            fileName = "a.bin",
            fileSize = 100L,
            targetCid = "0",
            input = ByteArrayInputStream(ByteArray(100)),
            onProgress = { _, _ -> },
        )

        assertTrue("expected Reject, got: $result", result is UploadOutcome.Reject)
        verify { ToastBus.error(match { it.contains("秒传") }) }
    }

    // ---------- (c) sign check 1 次成功 ----------

    @Test fun `status 6 then 1 success calls ossUploader once and reInitForSignCheck`() = runBlocking {
        // 第一次 init 返回 status=6 + sign_check="0-99" + sign_key="K1"
        val firstResp = UploadInitResp(
            status = 6, sign_key = "K1", sign_check = "0-99", file_id = "",
            target = "U_1_0", bucket = "b", `object` = "o",
            callback = UploadCallback.Single(CallbackInfo("", "")),
            pick_code = "pc",
        )
        // reInit 拿到 status=1 继续
        val secondResp = UploadInitResp(
            status = 1, sign_key = "", sign_check = "", file_id = "F",
            target = "U_1_0", bucket = "test-bucket", `object` = "test-object",
            callback = UploadCallback.Single(CallbackInfo("http://cb", "")),
            pick_code = "pc",
        )
        coEvery { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany listOf(
            Response.success(firstResp),
            Response.success(secondResp),
        )
        coEvery { api.getUploadToken() } returns Response.success(
            UploadGetTokenResp("https://oss-cn-shanghai.aliyuncs.com", "AK", "SK", "STS", "2026-06-25T12:00:00Z")
        )
        coEvery { ossUploader.upload(any(), any(), any(), any(), any(), any()) } returns Unit

        val result = executor.run(
            fileName = "a.bin",
            fileSize = 200L,  // 文件 >= 100 字节能 hold 区间
            targetCid = "0",
            input = ByteArrayInputStream(ByteArray(200)),
            onProgress = { _, _ -> },
        )

        assertTrue("expected Success, got: $result", result is UploadOutcome.Success)
        // 第一次 init + 第二次 reInit,共 2 次 initUpload 调用
        coVerify(exactly = 2) { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { ossUploader.upload(any(), any(), any(), any(), any(), any()) }
    }

    // ---------- (d) sign check 反复 → 文件校验失败 ----------

    @Test fun `status 6 then 6 again returns failure with 文件校验失败 toast`() = runBlocking {
        val firstResp = UploadInitResp(
            status = 6, sign_key = "K1", sign_check = "0-99", file_id = "",
            target = "U_1_0", bucket = "b", `object` = "o",
            callback = UploadCallback.Single(CallbackInfo("", "")),
            pick_code = "",
        )
        // reInit 仍然 status=6
        val secondResp = firstResp.copy(sign_key = "K2", sign_check = "100-199")
        coEvery { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany listOf(
            Response.success(firstResp),
            Response.success(secondResp),
        )

        val result = executor.run(
            fileName = "a.bin",
            fileSize = 500L,
            targetCid = "0",
            input = ByteArrayInputStream(ByteArray(500)),
            onProgress = { _, _ -> },
        )

        assertTrue("expected Failure, got: $result", result is UploadOutcome.Failure)
        verify { ToastBus.error(match { it.contains("校验失败") }) }
        // reInit 没让 OssUploader 被调
        coVerify(exactly = 0) { ossUploader.upload(any(), any(), any(), any(), any(), any()) }
    }

    // ---------- (e) OssUploader 抛 → 失败 + toast ----------

    @Test fun `ossUploader upload exception returns failure with toast`() = runBlocking {
        val initResp = UploadInitResp(
            status = 1, sign_key = "", sign_check = "", file_id = "F",
            target = "U_1_0", bucket = "test-bucket", `object` = "test-object",
            callback = UploadCallback.Single(CallbackInfo("http://cb", "")),
            pick_code = "",
        )
        coEvery { api.initUpload(any(), any(), any(), any(), any(), any(), any(), any()) } returns Response.success(initResp)
        coEvery { api.getUploadToken() } returns Response.success(
            UploadGetTokenResp("https://oss-cn-shanghai.aliyuncs.com", "AK", "SK", "STS", "2026-06-25T12:00:00Z")
        )
        coEvery { ossUploader.upload(any(), any(), any(), any(), any(), any()) } throws
            IOException("simulated OSS error")

        val result = executor.run(
            fileName = "a.bin",
            fileSize = 10L * 1024 * 1024,
            targetCid = "0",
            input = ByteArrayInputStream(ByteArray(10 * 1024 * 1024)),
            onProgress = { _, _ -> },
        )

        assertTrue("expected Failure, got: $result", result is UploadOutcome.Failure)
        // 失败 toast 调用
        verify { ToastBus.error(any<String>()) }
    }
}
