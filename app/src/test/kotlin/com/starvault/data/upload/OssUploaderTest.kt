package com.starvault.data.upload

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Base64

/**
 * OssUploader 单元测试 — 通过 [OssOperations] seam mock Aliyun OSS SDK,
 * 验证单 PUT / multipart 分流 + per-part 重试 + base64 callback 头。
 *
 * 关键约束(对齐 OpenList `drivers/115_open/upload.go`):
 *  - fileSize ≤ 20MB  → `putObject` 1 次
 *  - fileSize >  20MB  → `initMultipart` + (N × `uploadPart`) + `completeMultipart`
 *  - 每分片失败重试 3 次(第 4 次抛)
 *  - `callback` / `callbackVar` 必须 Base64 编码后塞到 `OssOperations` 参数
 *
 * 测试 seam:Aliyun OSS Java SDK 是 final class,直接 mockk 不稳,
 * 所以用 [OssOperations] interface 抽象(`AliyunOssOperations` 实现走真 SDK),
 * 测试用 [FakeOssOperations] 计数。
 */
class OssUploaderTest {

    @Before fun mockAndroidLog() {
        // android.util.Log 在 JVM 单元测试环境下抛 UnsatisfiedLinkError,
        // mockkStatic 把它变成 no-op(对齐 CLAUDE.md + ProfileViewModelTest 模式)
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    // ---------- 分流决策 ----------

    @Test fun `upload 10MB uses putObject single shot`() = runBlocking {
        val fake = FakeOssOperations()
        val uploader = OssUploader(ops = fake, maxRetries = 0, baseDelayMillis = 0L)

        uploader.upload(
            bucket = "test-bucket",
            key = "test-object",
            input = ByteArrayInputStream(ByteArray(10 * 1024 * 1024)),
            totalBytes = 10L * 1024 * 1024,
            callback = "http://cb.example.com",
            callbackVar = """{"k":"v"}""",
        )

        assertEquals(1, fake.putObjectCalls)
        assertEquals(0, fake.multipartInitCalls)
        assertEquals(0, fake.uploadPartCalls)
    }

    @Test fun `upload 25MB uses multipart with 20MB part size`() = runBlocking {
        // 25MB / 20MB = 2 parts (剩余 5MB 也算 1 part)
        val fake = FakeOssOperations()
        val uploader = OssUploader(ops = fake, maxRetries = 0, baseDelayMillis = 0L)

        uploader.upload(
            bucket = "b",
            key = "k",
            input = ByteArrayInputStream(ByteArray(25 * 1024 * 1024)),
            totalBytes = 25L * 1024 * 1024,
            callback = "http://cb",
            callbackVar = "",
        )

        assertEquals(1, fake.multipartInitCalls)
        assertEquals(2, fake.uploadPartCalls)
        assertEquals(1, fake.multipartCompleteCalls)
    }

    @Test fun `upload exactly 20MB uses putObject single shot`() = runBlocking {
        // 边界:20MB 整 → 1 分片
        val fake = FakeOssOperations()
        val uploader = OssUploader(ops = fake, maxRetries = 0, baseDelayMillis = 0L)

        uploader.upload(
            bucket = "b",
            key = "k",
            input = ByteArrayInputStream(ByteArray(20 * 1024 * 1024)),
            totalBytes = 20L * 1024 * 1024,
            callback = "",
            callbackVar = "",
        )

        assertEquals(1, fake.putObjectCalls)
    }

    // ---------- per-part retry ----------

    @Test fun `uploadPart retry on transient failure then success`() = runBlocking {
        // part 1 失败 2 次(剩 0),第 3 次成功;part 2 直接成功
        val fake = FakeOssOperations(
            uploadPartFailures = mapOf(1 to 2),  // part 1: 初始 remaining=2 → 失败 2 次
        )
        val uploader = OssUploader(ops = fake, maxRetries = 3, baseDelayMillis = 0L)

        uploader.upload(
            bucket = "b",
            key = "k",
            input = ByteArrayInputStream(ByteArray(25 * 1024 * 1024)),
            totalBytes = 25L * 1024 * 1024,
            callback = "http://cb",
            callbackVar = "",
        )

        // part 1 被调 3 次(2 fail + 1 success) + part 2 1 次成功 = 4
        assertEquals(4, fake.uploadPartCalls)
        // complete multipart 仍然被调(因为 part 1 第 3 次成功)
        assertEquals(1, fake.multipartCompleteCalls)
    }

    @Test fun `uploadPart exhausts maxRetries then propagates exception`() = runBlocking {
        // maxRetries=3 → 总共尝试 4 次,全部失败 → 抛
        val fake = FakeOssOperations(
            uploadPartFailures = mapOf(1 to 99),  // part 1: 永远失败
        )
        val uploader = OssUploader(ops = fake, maxRetries = 3, baseDelayMillis = 0L)

        var caught: Throwable? = null
        try {
            uploader.upload(
                bucket = "b",
                key = "k",
                input = ByteArrayInputStream(ByteArray(25 * 1024 * 1024)),
                totalBytes = 25L * 1024 * 1024,
                callback = "http://cb",
                callbackVar = "",
            )
        } catch (t: Throwable) {
            caught = t
        }

        // part 1: maxRetries(3) + 初次 = 4 次
        assertEquals(4, fake.uploadPartCalls)
        assertTrue("expected retry exception propagated, got: $caught", caught != null)
    }

    // ---------- base64 callback 头 ----------

    @Test fun `putObject encodes callback headers as base64`() = runBlocking {
        val fake = FakeOssOperations()
        val uploader = OssUploader(ops = fake, maxRetries = 0, baseDelayMillis = 0L)

        val rawCallback = "http://callback.example.com/path?x=1"
        val rawCallbackVar = """{"user":"alice"}"""

        uploader.upload(
            bucket = "b",
            key = "k",
            input = ByteArrayInputStream(ByteArray(10 * 1024 * 1024)),
            totalBytes = 10L * 1024 * 1024,
            callback = rawCallback,
            callbackVar = rawCallbackVar,
        )

        val encodedCallback = Base64.getEncoder().encodeToString(rawCallback.toByteArray())
        val encodedCallbackVar = Base64.getEncoder().encodeToString(rawCallbackVar.toByteArray())
        assertEquals(encodedCallback, fake.lastPutObjectCallbackB64)
        assertEquals(encodedCallbackVar, fake.lastPutObjectCallbackVarB64)
    }

    @Test fun `uploadMultipart encodes callback headers as base64`() = runBlocking {
        val fake = FakeOssOperations()
        val uploader = OssUploader(ops = fake, maxRetries = 0, baseDelayMillis = 0L)

        val rawCallback = "http://callback.example.com"
        val rawCallbackVar = """{"k":"v"}"""

        uploader.upload(
            bucket = "b",
            key = "k",
            input = ByteArrayInputStream(ByteArray(25 * 1024 * 1024)),
            totalBytes = 25L * 1024 * 1024,
            callback = rawCallback,
            callbackVar = rawCallbackVar,
        )

        val encodedCallback = Base64.getEncoder().encodeToString(rawCallback.toByteArray())
        val encodedCallbackVar = Base64.getEncoder().encodeToString(rawCallbackVar.toByteArray())
        assertEquals(encodedCallback, fake.lastUploadPartCallbackB64)
        assertEquals(encodedCallbackVar, fake.lastUploadPartCallbackVarB64)
    }
}

/**
 * 测试 seam:记录所有 OSS 操作调用,可选让特定 part 失败 N 次后成功。
 *
 * 设计原则:
 *  - 抛真实 [IOException],保证 OssUploader 的 catch 行为跟生产一致
 *  - 计数器 + slot 双轨:计数验证调用次数,slot 验证参数
 */
class FakeOssOperations(
    private val uploadPartFailures: Map<Int, Int> = emptyMap(),
) : OssOperations {

    var putObjectCalls = 0
        private set
    var multipartInitCalls = 0
        private set
    var uploadPartCalls = 0
        private set
    var multipartCompleteCalls = 0
        private set

    var lastPutObjectCallbackB64: String? = null
        private set
    var lastPutObjectCallbackVarB64: String? = null
        private set
    var lastUploadPartCallbackB64: String? = null
        private set
    var lastUploadPartCallbackVarB64: String? = null
        private set

    private val remainingFailures = HashMap(uploadPartFailures)

    override suspend fun putObject(
        bucket: String,
        key: String,
        data: ByteArray,
        callbackBase64: String,
        callbackVarBase64: String,
    ) {
        putObjectCalls++
        lastPutObjectCallbackB64 = callbackBase64
        lastPutObjectCallbackVarB64 = callbackVarBase64
    }

    override suspend fun initMultipart(
        bucket: String,
        key: String,
        callbackBase64: String,
        callbackVarBase64: String,
    ): String {
        multipartInitCalls++
        return "fake-upload-id-${multipartInitCalls}"
    }

    override suspend fun uploadPart(
        bucket: String,
        key: String,
        uploadId: String,
        partNumber: Int,
        data: ByteArray,
        callbackBase64: String,
        callbackVarBase64: String,
    ) {
        uploadPartCalls++
        lastUploadPartCallbackB64 = callbackBase64
        lastUploadPartCallbackVarB64 = callbackVarBase64
        // 当前 part 还有剩余失败次数 → 消耗一次,模拟瞬时失败
        val remaining = remainingFailures[partNumber] ?: 0
        if (remaining > 0) {
            remainingFailures[partNumber] = remaining - 1
            throw IOException("simulated transient OSS failure on part $partNumber (remaining=${remaining - 1})")
        }
    }

    override suspend fun completeMultipart(
        bucket: String,
        key: String,
        uploadId: String,
    ) {
        multipartCompleteCalls++
    }

    override suspend fun abortMultipart(bucket: String, key: String, uploadId: String) {
        // no-op for tests
    }
}
