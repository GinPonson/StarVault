package com.starvault.data.uploadworker

import android.util.Log
import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.CallbackInfo
import com.starvault.data.remote.cloud115.OpenUploadApiService
import com.starvault.data.remote.cloud115.UploadCallback
import com.starvault.data.remote.cloud115.UploadGetTokenResp
import com.starvault.data.remote.cloud115.UploadInitResp
import com.starvault.data.remote.cloud115.requireSuccessful
import com.starvault.data.upload.OssUploader
import com.starvault.data.upload.Sha1Hashing
import com.starvault.data.upload.UploadInitClient
import com.starvault.data.upload.sha1OfPrefix
import com.starvault.data.upload.sha1OfStream
import java.io.InputStream

/**
 * Upload 业务流程编排 — Init → State machine → Sign check 回路 → GetToken → OssUpload。
 *
 * ## 为什么拆出这个类
 *
 * [UploadWorker](CoroutinesWorker) 本身是 WorkManager 适配器,负责:
 *  - setProgress / setForeground
 *  - URI → InputStream 转换
 *  - 把 executor 返回的 [UploadOutcome] 映射成 `Result.success/failure`
 *
 * Worker 跑在 WorkManager runtime 上,单测需要 Robolectric。把核心编排(纯 IO + 业务决策)
 * 抽到本类后,核心 5 个 case(plan §3.1.4 + §8)在纯 JVM 就能 TDD。
 *
 * ## 进度回调契约
 *
 * [onProgress] 由 Worker 注入,内部调 `OssUploader.upload(..., onPart = ...)`。
 * 0..totalBytes 区间,Worker 据此 `setProgress(workDataOf("transferred" to N))`。
 */
class UploadExecutor(
    private val uploadInitClient: UploadInitClient,
    private val ossUploader: OssUploader,
    private val api: OpenUploadApiService,
) {

    /**
     * 跑完整上传流程(纯 IO + 决策,无 WorkManager 依赖)。
     *
     * @param input 整文件输入流(由调用方持有;executor 内部会 readBytes 一次 — 跟 OssUploader
     *              内存模型对齐)
     * @param onProgress suspend 回调 — [UploadWorker] 内部调 `setProgress(...)`(suspend)
     *                   需要 suspend 上下文
     */
    suspend fun run(
        fileName: String,
        fileSize: Long,
        targetCid: String,
        input: InputStream,
        onProgress: suspend (transferredBytes: Long, totalBytes: Long) -> Unit,
    ): UploadOutcome {
        return try {
            // 1. 算整文件 SHA1 + 前 128 KiB SHA1(M2 协议契约)
            val allBytes = input.readBytes()
            if (allBytes.size.toLong() != fileSize) {
                ToastBus.error("文件读取失败:大小不匹配")
                return UploadOutcome.Failure("size_mismatch")
            }
            val sha1 = sha1OfStream(ByteArrayInputStreamSource(allBytes), fileSize)
            val preSha1 = sha1OfPrefix(ByteArrayInputStreamSource(allBytes), 128 * 1024)

            // 2. Init
            var resp: UploadInitResp = uploadInitClient.init(
                fileName = fileName,
                fileSize = fileSize,
                targetCid = targetCid,
                sha1 = sha1,
                preSha1 = preSha1,
            )

            // 3. Status 分支 — 状态机决策
            when (val decision = UploadStateMachine.decide(resp)) {
                is UploadDecision.Continue -> {
                    // 4. Get STS token + 5. OSS upload
                    val token = getUploadToken()
                    val callbackInfo = extractCallbackInfo(decision.callback)
                    ossUploader.upload(
                        bucket = decision.bucket,
                        key = decision.ossObject,
                        input = ByteArrayInputStreamSource(allBytes),
                        totalBytes = fileSize,
                        callback = callbackInfo.callback,
                        callbackVar = callbackInfo.callback_var,
                        sts = token,
                    )
                    onProgress(fileSize, fileSize)
                    UploadOutcome.Success
                }
                is UploadDecision.Reject -> {
                    ToastBus.error(decision.message)
                    UploadOutcome.Reject(decision.message)
                }
                is UploadDecision.SignCheck -> {
                    // 算 sign_check 区间的 SHA1
                    val rangeSha1 = sha1OfPrefix(
                        ByteArrayInputStreamSource(allBytes),
                        decision.endInclusive.toInt() + 1,
                    )
                    // reInit
                    val reInitResp = uploadInitClient.reInitForSignCheck(
                        fileName = fileName,
                        fileSize = fileSize,
                        targetCid = targetCid,
                        sha1 = sha1,
                        preSha1 = preSha1,
                        pickCode = resp.pick_code,
                        signKey = decision.signKey,
                        signVal = rangeSha1,
                    )
                    // 递归走状态机(最多 2 次,防止死循环)
                    handleAfterSignCheck(
                        resp = reInitResp,
                        fileName = fileName,
                        fileSize = fileSize,
                        targetCid = targetCid,
                        sha1 = sha1,
                        preSha1 = preSha1,
                        allBytes = allBytes,
                        onProgress = onProgress,
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "upload failed", t)
            ToastBus.error(t.message ?: "上传失败")
            UploadOutcome.Failure(t.message ?: "unknown")
        }
    }

    /**
     * reInit 之后递归走状态机 — 第二个 status 决策。
     */
    private suspend fun handleAfterSignCheck(
        resp: UploadInitResp,
        fileName: String,
        fileSize: Long,
        targetCid: String,
        sha1: String,
        preSha1: String,
        allBytes: ByteArray,
        onProgress: suspend (Long, Long) -> Unit,
    ): UploadOutcome {
        return when (val decision = UploadStateMachine.decide(resp)) {
            is UploadDecision.Continue -> {
                val token = getUploadToken()
                val callbackInfo = extractCallbackInfo(decision.callback)
                ossUploader.upload(
                    bucket = decision.bucket,
                    key = decision.ossObject,
                    input = ByteArrayInputStreamSource(allBytes),
                    totalBytes = fileSize,
                    callback = callbackInfo.callback,
                    callbackVar = callbackInfo.callback_var,
                    sts = token,
                )
                onProgress(fileSize, fileSize)
                UploadOutcome.Success
            }
            is UploadDecision.Reject -> {
                ToastBus.error(decision.message)
                UploadOutcome.Reject(decision.message)
            }
            is UploadDecision.SignCheck -> {
                // 第二次 SignCheck → 文件校验失败(再 reInit 会进入死循环)
                ToastBus.error("文件校验失败,请重试")
                UploadOutcome.Failure("sign_check_repeated")
            }
        }
    }

    /**
     * 拿 STS token — 走 api 直调(M2 spec §6 步骤 4)。
     *
     * 拆 envelope:envelope.state == false 时抛 IllegalStateException(message),
     * 让外层 catch 转 ToastBus + UploadOutcome.Failure。
     */
    private suspend fun getUploadToken(): UploadGetTokenResp {
        val envelope = api.getUploadToken().requireSuccessful()
        if (!envelope.state) {
            throw IllegalStateException("get_token failed: code=${envelope.code} message=${envelope.message}")
        }
        return envelope.data
    }

    /**
     * 从 [UploadCallback](sealed) 抽取 callback + callbackVar 字符串(给 OssUploader)。
     */
    private fun extractCallbackInfo(callback: UploadCallback): CallbackInfo = when (callback) {
        is UploadCallback.Single -> callback.value
        is UploadCallback.Multi -> callback.items.firstOrNull()
            ?: CallbackInfo("", "")
    }

    private companion object {
        const val TAG = "UploadExecutor"
    }
}

/**
 * Executor 运行结果 — 映射到 [UploadWorker] 的 `Result.success/failure`。
 */
sealed interface UploadOutcome {
    /** 上传成功(OSS 写入完成)。 */
    data object Success : UploadOutcome

    /** 业务拒绝(秒传 / 校验失败 / 初始化失败) — Worker 仍然 Result.failure + 已发 toast。 */
    data class Reject(val message: String) : UploadOutcome

    /** 系统异常(OAuth / 网络 / OSS IO) — Worker 仍然 Result.failure + 已发 toast。 */
    data class Failure(val cause: String) : UploadOutcome
}

/**
 * ByteArray → InputStream 适配器(让 sha1OfStream / sha1OfPrefix / OssUploader 都能消费同一份 byte[])。
 *
 * 把 byte[] 重复包 ByteArrayInputStream 的成本可以忽略(M2 文件 < 5GB,几 MB 分配),
 * 比把 [OssUploader] / [Sha1Hashing] 接口改成 ByteArray 简单。
 */
private fun ByteArrayInputStreamSource(bytes: ByteArray): java.io.InputStream =
    java.io.ByteArrayInputStream(bytes)
