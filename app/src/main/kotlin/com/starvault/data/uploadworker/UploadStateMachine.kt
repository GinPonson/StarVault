package com.starvault.data.uploadworker

import com.starvault.data.remote.cloud115.UploadInitResp

/**
 * 115 UploadInit status 状态机决策 — 纯函数,无 Android / 协程依赖。
 *
 * ## 决策表(spec §3.1.4)
 *
 *  | Status  | 决策                | Worker 行为                        |
 *  |---------|---------------------|-------------------------------------|
 *  | 1       | [UploadDecision.Continue]  | 走 OSS 上传                |
 *  | 2       | [UploadDecision.Reject]    | ToastBus.error("暂不支持秒传") |
 *  | 6/7/8   | [UploadDecision.SignCheck] | 算区间 SHA1,reInitForSignCheck |
 *  | 其它    | [UploadDecision.Reject]    | ToastBus.error("初始化失败")  |
 *
 * ## 设计意图
 *
 * 把 status 分支提到纯函数(本类),[UploadWorker] 业务代码 ≈
 * "init → decide → switch 分支"线性,Worker 本身只是 IO + 进度上报 + ForegroundInfo
 * 协调器。把控制流 + 解析逻辑(spec §6 步骤 3a "parse signCheck=start-end")
 * 跟 IO 解耦后,Worker 单元测试只需 stub 3 个 collaborator
 * (UploadInitClient / Sha1Hashing / OssUploader),不需要 Robolectric。
 */
object UploadStateMachine {

    /**
     * 决策 [UploadInitResp.status] 的下一步动作。
     */
    fun decide(resp: UploadInitResp): UploadDecision = when (resp.status) {
        1 -> UploadDecision.Continue(
            bucket = resp.bucket,
            ossObject = resp.`object`,
            pickCode = resp.pick_code,
            callback = resp.callback,
        )
        2 -> UploadDecision.Reject("暂不支持秒传")
        6, 7, 8 -> {
            val range = parseSignRange(resp.sign_check)
            UploadDecision.SignCheck(
                signKey = resp.sign_key,
                startInclusive = range.first,
                endInclusive = range.last,
            )
        }
        else -> UploadDecision.Reject("初始化失败:status=${resp.status}")
    }

    /**
     * 解析 `sign_check` 字段 = "start-end" 格式(inclusive,字节偏移)。
     *
     * @throws UploadDecisionException 格式不合法时
     */
    fun parseSignRange(raw: String): LongRange {
        val dash = raw.indexOf('-')
        if (dash <= 0 || dash == raw.length - 1) {
            throw UploadDecisionException("malformed sign_check range: $raw")
        }
        val start = raw.substring(0, dash).toLongOrNull()
        val end = raw.substring(dash + 1).toLongOrNull()
        if (start == null || end == null || end < start) {
            throw UploadDecisionException("malformed sign_check range: $raw")
        }
        return start..end
    }
}

/**
 * 状态机决策结果 — sealed interface,Worker switch 时编译器强制 exhaustive。
 */
sealed interface UploadDecision {

    /**
     * 正常走 OSS 上传路径(status=1)。
     *
     * @param callback 服务端要求的 115 callback 头(已经在 resp 里,Worker 不再 Base64 一次 —
     *                 OssUploader 内部统一 Base64)
     */
    data class Continue(
        val bucket: String,
        val ossObject: String,
        val pickCode: String,
        val callback: com.starvault.data.remote.cloud115.UploadCallback,
    ) : UploadDecision

    /**
     * 走 two-way verify(status=6/7/8)。
     *
     * Worker 收到这个决策后:
     *  1. URI seek 到 [startInclusive]
     *  2. 读 [endInclusive] - [startInclusive] + 1 字节
     *  3. SHA1 → uppercase hex
     *  4. `uploadInitClient.reInitForSignCheck(... signKey, signVal = this hash)`
     *  5. 拿到的 resp 再次 [UploadStateMachine.decide]
     */
    data class SignCheck(
        val signKey: String,
        val startInclusive: Long,
        val endInclusive: Long,
    ) : UploadDecision

    /**
     * 终止上传(秒传 / 未知 status)。
     *
     * @param message 给用户的可读错误(Worker 走 ToastBus.error 直接展示)
     */
    data class Reject(val message: String) : UploadDecision
}

class UploadDecisionException(message: String) : RuntimeException(message)
