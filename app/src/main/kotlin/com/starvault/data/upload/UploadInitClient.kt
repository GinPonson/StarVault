package com.starvault.data.upload

import com.starvault.data.remote.cloud115.OpenUploadApiService
import com.starvault.data.remote.cloud115.UploadInitResp
import com.starvault.data.remote.cloud115.requireSuccessful

/**
 * 115 上传 init 客户端 — 薄壳包 [OpenUploadApiService.initUpload],负责:
 *
 *  1. 拼 `target = "U_1_$targetCid"` 完整前缀(OpenList `drivers/115_open/upload.go::UploadInit`)
 *  2. `file_size` Long → String(协议契约,服务端按字符串解析)
 *  3. **不**发送 `topupload` 字段 — 115 官方文档"必填:否",Go SDK 调用时不带此 key
 *  4. 首次上传 `pick_code=""`(M2 没有断点续传);二次认证时由调用方传 [reInitForSignCheck]
 *  5. **拆 envelope**:115 proapi 端点统一返回 `{state, code, message, data}` 4 字段,
 *     业务负载在 `data` 里。本类拿 envelope 后拆 `.data` 给 [UploadStateMachine] 用;
 *     `envelope.state == false` 抛 IllegalStateException 让 worker 走 Reject 分支。
 *
 * 使用方式:
 * ```
 * val resp = client.init(fileName, fileSize, targetCid = currentCid, sha1, preSha1)
 * when (resp.status) {
 *   1 -> uploadToOss(...)
 *   2 -> fail("暂不支持秒传")
 *   6, 7, 8 -> reInitForSignCheck(...)
 * }
 * ```
 *
 * 错误语义:
 *  - HTTP 非 2xx → [requireSuccessful] 抛 IllegalStateException("HTTP {code}")
 *  - envelope.state == false → 抛 IllegalStateException(envelope.message)
 *  - 两种异常在 [UploadWorker.doWork] catch 后走 Result.failure + ToastBus
 */
class UploadInitClient(
    private val api: OpenUploadApiService,
) {

    /**
     * 首次 init / 普通 init。
     *
     * @param targetCid 目标目录 cid(root = "0");内部拼成 `U_1_<cid>`
     */
    suspend fun init(
        fileName: String,
        fileSize: Long,
        targetCid: String,
        sha1: String,
        preSha1: String,
    ): UploadInitResp {
        val envelope = api.initUpload(
            file_name = fileName,
            file_size = fileSize.toString(),  // 协议:String
            target = U1_PREFIX + targetCid,
            fileid = sha1,
            preid = preSha1,
            pick_code = "",  // M2 首次上传,无 pick_code
            sign_key = "",   // 首次 init 无 sign
            sign_val = "",
        ).requireSuccessful()
        if (!envelope.state) {
            throw IllegalStateException("init failed: code=${envelope.code} message=${envelope.message}")
        }
        return envelope.data
    }

    /**
     * 二次认证 re-init(status=6/7/8 时调用)。
     *
     * 服务端根据 [signKey] / [signVal] / [pickCode] 校验文件区间 SHA1,然后:
     *  - 成功 → 返回 status=1 继续 OSS 上传
     *  - 失败 → 返回 status=6/7/8 → 调用方再 re-init,或放弃
     */
    suspend fun reInitForSignCheck(
        fileName: String,
        fileSize: Long,
        targetCid: String,
        sha1: String,
        preSha1: String,
        pickCode: String,
        signKey: String,
        signVal: String,
    ): UploadInitResp {
        val envelope = api.initUpload(
            file_name = fileName,
            file_size = fileSize.toString(),
            target = U1_PREFIX + targetCid,
            fileid = sha1,
            preid = preSha1,
            pick_code = pickCode,
            sign_key = signKey,
            sign_val = signVal,
        ).requireSuccessful()
        if (!envelope.state) {
            throw IllegalStateException("reInit failed: code=${envelope.code} message=${envelope.message}")
        }
        return envelope.data
    }

    private companion object {
        /**
         * 115 upload 协议 `target` 字段固定前缀 `U_1_`。
         *
         * 来源:OpenList `drivers/115_open/upload.go`:`target: "U_1_" + cId`(cid = 目录 id,
         * 0 = 根目录)。M2 spec §3.1.2 显式要求拼好后传给 init。
         */
        const val U1_PREFIX = "U_1_"
    }
}
