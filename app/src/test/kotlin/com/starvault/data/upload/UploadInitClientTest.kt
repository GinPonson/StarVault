package com.starvault.data.upload

import com.starvault.data.remote.cloud115.CallbackInfo
import com.starvault.data.remote.cloud115.OpenUploadApiService
import com.starvault.data.remote.cloud115.UploadCallback
import com.starvault.data.remote.cloud115.UploadInitEnvelope
import com.starvault.data.remote.cloud115.UploadInitResp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Response

/**
 * UploadInitClient 单元测试 — mock OpenUploadApiService 拦截 initUpload 调用,
 * 验证 target 前缀拼接 + 协议字段不发送(`topupload` 不在方法签名里)。
 *
 * 关键约束:
 *  - `target` 必须拼成 `U_1_<cid>` 完整格式(115 协议,OpenList 1:1)
 *  - M2 单文件场景不发送 `topupload` 字段(对齐 115 官方文档 "必填:否" + Go SDK 调用时不发)
 *  - `pick_code` / `sign_key` / `sign_val` 默认空串(M2 首次上传 + 未触发二次认证)
 *  - 二次认证 re-init 必须把 signKey/signVal 透传
 */
class UploadInitClientTest {

    private val api: OpenUploadApiService = mockk(relaxed = false)
    private val client = UploadInitClient(api)

    private fun stubInit(status: Int = 1): UploadInitResp = UploadInitResp(
        status = status,
        sign_key = "",
        sign_check = "",
        file_id = "",
        target = "U_1_0",
        bucket = "b",
        `object` = "o",
        callback = UploadCallback.Single(
            CallbackInfo("http://cb", ""),
        ),
        pick_code = "pc",
    )

    /** Wrap [UploadInitResp] in envelope (115 proapi 统一 `{state, code, message, data}`)。 */
    private fun env(r: UploadInitResp) =
        UploadInitEnvelope(state = true, code = 0, message = "", data = r)

    @Test fun `init prefixes U_1_ on target and sends no topupload field`() = runBlocking {
        // init() 内部 target = "U_1_$cid",不暴露 target 参数;调用方只传 cid
        coEvery { api.initUpload(
            file_name = "a.bin",
            file_size = "1234567",
            target = "U_1_42",
            fileid = "DEAD",
            preid = "BEEF",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        ) } returns Response.success(env(stubInit()))

        val resp = client.init(
            fileName = "a.bin",
            fileSize = 1234567L,
            targetCid = "42",
            sha1 = "DEAD",
            preSha1 = "BEEF",
        )

        assertNotNull(resp)
        // 拦截调用 — 确保 target 是 "U_1_42" 而非裸 "42"
        coVerify(exactly = 1) {
            api.initUpload(
                file_name = "a.bin",
                file_size = "1234567",
                target = "U_1_42",
                fileid = "DEAD",
                preid = "BEEF",
                pick_code = "",
                sign_key = "",
                sign_val = "",
            )
        }
    }

    @Test fun `init with targetCid zero produces U_1_0 target`() = runBlocking {
        // 根目录 cid = "0" → target = "U_1_0"(OpenList 约定)
        coEvery { api.initUpload(
            file_name = "x.bin",
            file_size = "100",
            target = "U_1_0",
            fileid = "A",
            preid = "B",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        ) } returns Response.success(env(stubInit()))

        client.init(
            fileName = "x.bin",
            fileSize = 100L,
            targetCid = "0",
            sha1 = "A",
            preSha1 = "B",
        )

        coVerify(exactly = 1) {
            api.initUpload(
                target = "U_1_0",
                file_name = "x.bin",
                file_size = "100",
                fileid = "A",
                preid = "B",
                pick_code = "",
                sign_key = "",
                sign_val = "",
            )
        }
    }

    @Test fun `init sends fileSize as decimal string literal`() = runBlocking {
        // 协议契约:file_size 在 wire 上是 STRING(Go SDK strconv.FormatInt(fileSize, 10))
        // 测试 fileSize = 12345 → 期望 "12345"(不是 "12.3K" 也不是 long)
        coEvery { api.initUpload(
            file_name = "f.bin",
            file_size = "12345",
            target = "U_1_0",
            fileid = "X",
            preid = "Y",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        ) } returns Response.success(env(stubInit()))

        client.init(
            fileName = "f.bin",
            fileSize = 12345L,
            targetCid = "0",
            sha1 = "X",
            preSha1 = "Y",
        )

        coVerify(exactly = 1) {
            api.initUpload(file_size = "12345", file_name = "f.bin", target = "U_1_0",
                fileid = "X", preid = "Y", pick_code = "", sign_key = "", sign_val = "")
        }
    }

    @Test fun `reInitForSignCheck populates signKey signVal and pickCode`() = runBlocking {
        // 二次认证触发后,reInitForSignCheck 透传 signKey + signVal + pickCode(从上次 init resp)
        coEvery { api.initUpload(
            file_name = "f.bin",
            file_size = "100",
            target = "U_1_0",
            fileid = "X",
            preid = "Y",
            pick_code = "pc-from-prev",
            sign_key = "K-1",
            sign_val = "RANGE-HASH",
        ) } returns Response.success(env(stubInit()))

        client.reInitForSignCheck(
            fileName = "f.bin",
            fileSize = 100L,
            targetCid = "0",
            sha1 = "X",
            preSha1 = "Y",
            pickCode = "pc-from-prev",
            signKey = "K-1",
            signVal = "RANGE-HASH",
        )

        coVerify(exactly = 1) {
            api.initUpload(
                file_name = "f.bin",
                file_size = "100",
                target = "U_1_0",
                fileid = "X",
                preid = "Y",
                pick_code = "pc-from-prev",
                sign_key = "K-1",
                sign_val = "RANGE-HASH",
            )
        }
    }

    @Test fun `init returns parsed UploadInitResp with status propagated`() = runBlocking {
        // 验证返回的 UploadInitResp.status 透传 — caller 据此决定 1/2/6/7/8 分支
        val stubbed = stubInit(status = 6)
        coEvery { api.initUpload(
            file_name = "f.bin",
            file_size = "100",
            target = "U_1_0",
            fileid = "X",
            preid = "Y",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        ) } returns Response.success(env(stubbed))

        val resp = client.init(
            fileName = "f.bin",
            fileSize = 100L,
            targetCid = "0",
            sha1 = "X",
            preSha1 = "Y",
        )

        assertEquals(6, resp.status)
    }
}
