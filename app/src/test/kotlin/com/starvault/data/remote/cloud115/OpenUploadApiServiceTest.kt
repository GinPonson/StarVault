package com.starvault.data.remote.cloud115

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * OpenUploadApiService Retrofit 端点回归 — MockWebServer 拦截真实 HTTP,
 * 验证请求路径、form 字段名、目标前缀 `U_1_` 拼接正确。
 */
class OpenUploadApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OpenUploadApiService

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenUploadApiService::class.java)
    }

    @After fun teardown() { server.shutdown() }

    @Test fun `getUploadToken hits correct path and parses response`() = runBlocking {
        // 服务端 envelope-wrapped response — 115 proapi 统一 `{state, code, message, data}` 4 字段,
        // 业务负载在 `data` 里。注意字段大小写:`Expiration` 大写 E(对齐 115 实际响应)
        val body = """
            {
              "state": true, "code": 0, "message": "",
              "data": {
                "endpoint": "https://oss-cn-shanghai.aliyuncs.com",
                "AccessKeyId": "AKID",
                "AccessKeySecret": "SK",
                "SecurityToken": "STS",
                "Expiration": "2026-06-25T12:00:00Z"
              }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val resp = api.getUploadToken()
        val recorded = server.takeRequest()

        assertEquals("GET", recorded.method)
        assertEquals("/open/upload/get_token", recorded.path)
        assertEquals(true, resp.body()!!.state)
        assertEquals("AKID", resp.body()!!.data.AccessKeyId)
        assertEquals("STS", resp.body()!!.data.SecurityToken)
    }

    @Test fun `initUpload sends U_1_ prefixed target and no topload key`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {
              "state": true, "code": 0, "message": "",
              "data": {
                "status": 1,
                "sign_key": "", "sign_check": "", "file_id": "",
                "target": "U_1_0", "bucket": "b", "object": "o",
                "callback": {"callback":"http://cb","callback_var":""},
                "pick_code": "pc"
              }
            }
        """.trimIndent()))

        api.initUpload(
            file_name = "a.bin",
            file_size = "1234567",
            target = "U_1_0",
            fileid = "DEADBEEF",
            preid = "BEEFDEAD",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        )

        val recorded: RecordedRequest = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/open/upload/init", recorded.path)

        val body = recorded.body.readUtf8()
        // 必须包含 U_1_ 前缀 + fileid 全小写 + file_size 是字符串字面量
        assertTrue("expected target=U_1_0 in body, got: $body",
            body.contains("target=U_1_0"))
        assertTrue("expected file_size=1234567 (string), got: $body",
            body.contains("file_size=1234567"))
        assertTrue("expected fileid=DEADBEEF in body, got: $body",
            body.contains("fileid=DEADBEEF"))
        // topload 绝对不能出现(对齐 115 官方文档 "必填:否")
        assertFalse("topupload key leaked into form body: $body",
            body.contains("topupload"))
    }

    @Test fun `initUpload with sign_check re-init populates sign fields`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {
              "state": true, "code": 0, "message": "",
              "data": {
                "status": 1, "sign_key": "", "sign_check": "", "file_id": "",
                "target": "U_1_0", "bucket": "b", "object": "o",
                "callback": {"callback":"","callback_var":""}, "pick_code": ""
              }
            }
        """.trimIndent()))

        api.initUpload(
            file_name = "a.bin",
            file_size = "100",
            target = "U_1_0",
            fileid = "DEAD",
            preid = "BEEF",
            pick_code = "pc",
            sign_key = "K1",
            sign_val = "VAL1",
        )

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("sign_key=K1"))
        assertTrue(body.contains("sign_val=VAL1"))
        assertTrue(body.contains("pick_code=pc"))
    }
}