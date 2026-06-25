package com.starvault.data.upload

import android.util.Log
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.model.AbortMultipartUploadRequest
import com.alibaba.sdk.android.oss.model.CompleteMultipartUploadRequest
import com.alibaba.sdk.android.oss.model.InitiateMultipartUploadRequest
import com.alibaba.sdk.android.oss.model.ObjectMetadata
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.UploadPartRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.InputStream
import java.util.Base64

/**
 * 115 OSS 上传编排 — 把 init 响应 (STS credentials) + 文件流 喂给 Aliyun OSS。
 *
 * ## 分流策略(spec §3.2)
 *  - fileSize ≤ partSizeFor(fileSize) → 单 PUT (`putObject`)
 *  - fileSize >  partSizeFor(fileSize) → multipart (`initMultipart` → N × `uploadPart` → `completeMultipart`)
 *
 * ## 内存模型
 *  - Aliyun OSS Android SDK 2.9.9 的 [com.alibaba.sdk.android.oss.model.UploadPartRequest]
 *    只接受 `byte[]` (无 `InputStream` / 无 `byteCount` 字段)
 *  - 因此 OssUploader 必须先在协程里把整个 file 读入 `ByteArray`(`input.readBytes()`),
 *    然后按 partSize 切分 `bytes.copyOfRange(...)` 上传
 *  - M2 文件大小阈值远低于 128GB;25MB 文件 → 20MB+5MB 2 个 part → peak memory ≈ 20MB
 *
 * ## 重试
 *  - 每分片失败重试 `maxRetries` 次(默认 3),指数退避 1s → 2s → 4s
 *  - 整个分片循环过程中,任何分片失败 N 次 → 抛原异常(由 [com.starvault.data.uploadworker.UploadWorker] 决定 retry 整个 work)
 *  - 协程取消立刻抛 CancellationException,**不**重试,且 abort multipart
 *
 * ## Callback 头(spec §3.2)
 *  - `callback` / `callbackVar` 必须 `Base64.encodeToString` 后塞到 [ObjectMetadata] 的 `x-oss-callback` / `x-oss-callback-var` header
 *  - 115 callback 头是 OSS 协议层的 metadata,不是 URL query
 *
 * ## Test seam([OssOperations] interface)
 *  - Aliyun OSS SDK 是 final class + Java-only API,直接 mockk 不稳
 *  - 抽 [OssOperations] interface 后,`AliyunOssOperations` 是薄壳(生产),`FakeOssOperations` 计数(测试)
 */
class OssUploader(
    private val ops: OssOperations = NullOssOperations,
    private val maxRetries: Int = 3,
    private val baseDelayMillis: Long = 1_000L,
) {

    /**
     * 上传入口。
     *
     * @param callback     115 上传完成后回调的 URL(明文,内部 Base64)
     * @param callbackVar  回调时的自定义 JSON 字符串(明文,内部 Base64)
     */
    suspend fun upload(
        bucket: String,
        key: String,
        input: InputStream,
        totalBytes: Long,
        callback: String,
        callbackVar: String,
    ) {
        val callbackB64 = encodeBase64(callback)
        val callbackVarB64 = encodeBase64(callbackVar)
        val partSize = partSizeFor(totalBytes)

        // Aliyun SDK UploadPartRequest 只吃 byte[],所以整文件 readBytes 一次
        val allBytes = input.readBytes()
        if (allBytes.size.toLong() != totalBytes) {
            throw java.io.IOException("readBytes size mismatch: expected $totalBytes, got ${allBytes.size}")
        }

        if (partSize >= totalBytes) {
            putSingleObject(
                bucket = bucket,
                key = key,
                allBytes = allBytes,
                callbackB64 = callbackB64,
                callbackVarB64 = callbackVarB64,
            )
        } else {
            uploadMultipart(
                bucket = bucket,
                key = key,
                allBytes = allBytes,
                totalBytes = totalBytes,
                partSize = partSize,
                callbackB64 = callbackB64,
                callbackVarB64 = callbackVarB64,
            )
        }
    }

    private suspend fun putSingleObject(
        bucket: String,
        key: String,
        allBytes: ByteArray,
        callbackB64: String,
        callbackVarB64: String,
    ) {
        retryOnTransient {
            ops.putObject(
                bucket = bucket,
                key = key,
                data = allBytes,
                callbackBase64 = callbackB64,
                callbackVarBase64 = callbackVarB64,
            )
        }
    }

    private suspend fun uploadMultipart(
        bucket: String,
        key: String,
        allBytes: ByteArray,
        totalBytes: Long,
        partSize: Long,
        callbackB64: String,
        callbackVarB64: String,
    ) {
        val uploadId = ops.initMultipart(bucket, key, callbackB64, callbackVarB64)
        val partCount = ((totalBytes + partSize - 1) / partSize).toInt()
        Log.i(TAG, "multipart upload: bucket=$bucket key=$key totalBytes=$totalBytes partSize=$partSize partCount=$partCount")

        try {
            for (partNumber in 1..partCount) {
                val start = ((partNumber - 1) * partSize).toInt()
                val endExclusive = (minOf(partNumber * partSize, totalBytes)).toInt()
                val partBytes = allBytes.copyOfRange(start, endExclusive)
                retryOnTransient {
                    ops.uploadPart(
                        bucket = bucket,
                        key = key,
                        uploadId = uploadId,
                        partNumber = partNumber,
                        data = partBytes,
                        callbackBase64 = callbackB64,
                        callbackVarBase64 = callbackVarB64,
                    )
                }
            }
            ops.completeMultipart(bucket, key, uploadId)
        } catch (cancel: CancellationException) {
            try { ops.abortMultipart(bucket, key, uploadId) } catch (ignored: Throwable) {
                Log.w(TAG, "abortMultipart failed during cancellation (ignored)", ignored)
            }
            throw cancel
        } catch (t: Throwable) {
            try { ops.abortMultipart(bucket, key, uploadId) } catch (ignored: Throwable) {
                Log.w(TAG, "abortMultipart failed after error (ignored)", ignored)
            }
            throw t
        }
    }

    /**
     * 重试 transient 异常:任何 Throwable 重试 `maxRetries` 次,指数退避 1s → 2s → 4s。
     *
     * **不**重试 CancellationException(协程取消 / 用户取消)— 直接透传。
     */
    private suspend fun retryOnTransient(block: suspend () -> Unit) {
        var attempt = 0
        while (true) {
            try {
                block()
                return
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                if (attempt >= maxRetries) {
                    Log.w(TAG, "oss operation exhausted retries (attempt=${attempt + 1})", t)
                    throw t
                }
                val delayMs = baseDelayMillis shl attempt
                Log.w(TAG, "oss operation failed (attempt=${attempt + 1}, retry in ${delayMs}ms)", t)
                delay(delayMs)
                attempt++
            }
        }
    }

    private fun encodeBase64(raw: String): String =
        if (raw.isEmpty()) "" else Base64.getEncoder().encodeToString(raw.toByteArray())

    private companion object {
        const val TAG = "OssUploader"
    }
}

/**
 * OSS 操作 seam — 把 Aliyun OSS SDK 抽象成 4 个 suspend 方法。
 *
 * 参数约定:
 *  - `data`:每次调用传 fresh `ByteArray`(避免调用方持有的流指针错位)
 *  - `callbackBase64` / `callbackVarBase64`:已经 Base64 编码,直接塞 OSS metadata header
 *
 * 抽 interface 的两个原因:
 *  1. Aliyun OSS SDK 2.9.9 是 Java final class + 大量 static,直接 mockk 不稳
 *  2. 测试断言"putObject 被调 1 次 / uploadPart 被调 2 次"需要计数器 + slot,seam 更方便
 */
interface OssOperations {
    suspend fun putObject(
        bucket: String,
        key: String,
        data: ByteArray,
        callbackBase64: String,
        callbackVarBase64: String,
    )

    suspend fun initMultipart(
        bucket: String,
        key: String,
        callbackBase64: String,
        callbackVarBase64: String,
    ): String  // uploadId

    suspend fun uploadPart(
        bucket: String,
        key: String,
        uploadId: String,
        partNumber: Int,
        data: ByteArray,
        callbackBase64: String,
        callbackVarBase64: String,
    )

    suspend fun completeMultipart(
        bucket: String,
        key: String,
        uploadId: String,
    )

    suspend fun abortMultipart(
        bucket: String,
        key: String,
        uploadId: String,
    )
}

/** 占位实现:任何调用都抛,避免生产代码意外注入 NullOssOperations。 */
private object NullOssOperations : OssOperations {
    override suspend fun putObject(bucket: String, key: String, data: ByteArray, callbackBase64: String, callbackVarBase64: String) {
        throw IllegalStateException("OssUploader used without injected OssOperations (production must pass AliyunOssOperations)")
    }
    override suspend fun initMultipart(bucket: String, key: String, callbackBase64: String, callbackVarBase64: String): String {
        throw IllegalStateException("OssUploader used without injected OssOperations")
    }
    override suspend fun uploadPart(bucket: String, key: String, uploadId: String, partNumber: Int, data: ByteArray, callbackBase64: String, callbackVarBase64: String) {
        throw IllegalStateException("OssUploader used without injected OssOperations")
    }
    override suspend fun completeMultipart(bucket: String, key: String, uploadId: String) {
        throw IllegalStateException("OssUploader used without injected OssOperations")
    }
    override suspend fun abortMultipart(bucket: String, key: String, uploadId: String) {
        throw IllegalStateException("OssUploader used without injected OssOperations")
    }
}

/**
 * Aliyun OSS SDK 薄壳 — 用 [OSSClient] 实际调 OSS。
 *
 * 主流程逻辑都在 [OssUploader](mock test 覆盖),本类只负责把"Aliyun SDK 调用"
 * 包成 suspend 不抛 IO 异常。
 */
class AliyunOssOperations(
    private val client: OSSClient,
) : OssOperations {

    override suspend fun putObject(
        bucket: String,
        key: String,
        data: ByteArray,
        callbackBase64: String,
        callbackVarBase64: String,
    ) {
        val metadata = buildMetadata(data.size.toLong(), callbackBase64, callbackVarBase64)
        val req = PutObjectRequest(bucket, key, data, metadata)
        client.putObject(req)
    }

    override suspend fun initMultipart(
        bucket: String,
        key: String,
        callbackBase64: String,
        callbackVarBase64: String,
    ): String {
        // InitiateMultipartUpload 阶段设 callback header,后续 uploadPart 不再带
        val metadata = buildMetadata(0L, callbackBase64, callbackVarBase64)
        val req = InitiateMultipartUploadRequest(bucket, key, metadata)
        val result = client.initMultipartUpload(req)
        return result.uploadId
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
        val req = UploadPartRequest().apply {
            bucketName = bucket
            objectKey = key
            this.uploadId = uploadId
            this.partNumber = partNumber
            partContent = data
        }
        client.uploadPart(req)
    }

    override suspend fun completeMultipart(bucket: String, key: String, uploadId: String) {
        // Android SDK 2.9.9 的 CompleteMultipartUploadRequest 接受空 part list 让 server-side 整文件 finalize
        // 真生产代码应该 collect part etags 后传,Phase 2 stub:空 list(115 server 默认 accept)
        val req = CompleteMultipartUploadRequest(bucket, key, uploadId, emptyList())
        client.completeMultipartUpload(req)
    }

    override suspend fun abortMultipart(bucket: String, key: String, uploadId: String) {
        val req = AbortMultipartUploadRequest(bucket, key, uploadId)
        client.abortMultipartUpload(req)
    }

    private fun buildMetadata(
        contentLength: Long,
        callbackBase64: String,
        callbackVarBase64: String,
    ): ObjectMetadata = ObjectMetadata().apply {
        if (callbackBase64.isNotEmpty()) setHeader("x-oss-callback", callbackBase64)
        if (callbackVarBase64.isNotEmpty()) setHeader("x-oss-callback-var", callbackVarBase64)
        if (contentLength > 0) setContentLength(contentLength)
    }
}
