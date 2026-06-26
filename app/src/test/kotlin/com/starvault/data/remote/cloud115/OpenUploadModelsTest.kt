package com.starvault.data.remote.cloud115

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 115 上传 DTO 序列化 / 反序列化回归 — 直接对协议 JSON 字段大小写 / 数组形态做断言。
 *
 * 真实来源:`https://www.yuque.com/115yun/open/ul4mrauo5i2uza0q`(文件上传)+ 115-sdk-go/upload.go。
 *
 * 这些测试是 Phase 1 的 RED 起点 —— 任何字段名大小写、`callback` 数组形态、
 * `file_size` 是 String 不是 Long 的偏差都会先在这里炸出来,而不是静默协议错。
 */
class OpenUploadModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test fun `UploadGetTokenResp parses case-sensitive JSON keys`() {
        // 故意大小写敏感:服务端驼峰 `AccessKeyId` / `AccessKeySecret` / `SecurityToken` / `Expiration`
        // (Expiration 大写 E — 对齐 115 实际响应,实测 2026-06-26 AVD 抓包)
        val src = """
            {
              "endpoint": "https://oss-cn-shanghai.aliyuncs.com",
              "AccessKeyId": "AKID-test",
              "AccessKeySecret": "SK-test",
              "SecurityToken": "STS-token",
              "Expiration": "2026-06-25T12:00:00Z"
            }
        """.trimIndent()
        val resp = json.decodeFromString(UploadGetTokenResp.serializer(), src)

        assertEquals("https://oss-cn-shanghai.aliyuncs.com", resp.endpoint)
        assertEquals("AKID-test", resp.AccessKeyId)
        assertEquals("SK-test", resp.AccessKeySecret)
        assertEquals("STS-token", resp.SecurityToken)
        assertEquals("2026-06-25T12:00:00Z", resp.Expiration)
    }

    @Test fun `UploadGetTokenEnvelope parses wrapped response`() {
        // 115 proapi 统一返回 envelope `{state, code, message, data}`
        val src = """
            {
              "state": true,
              "code": 0,
              "message": "",
              "data": {
                "endpoint": "https://oss-cn-shenzhen.aliyuncs.com",
                "AccessKeyId": "STS.NYfb",
                "AccessKeySecret": "5sKs",
                "SecurityToken": "CAIS...",
                "Expiration": "2026-06-26T03:59:31Z"
              }
            }
        """.trimIndent()
        val env = json.decodeFromString(UploadGetTokenEnvelope.serializer(), src)

        assertEquals(true, env.state)
        assertEquals(0, env.code)
        assertEquals("STS.NYfb", env.data.AccessKeyId)
        assertEquals("CAIS...", env.data.SecurityToken)
    }

    @Test fun `UploadInitEnvelope parses wrapped response`() {
        val src = """
            {
              "state": true,
              "code": 0,
              "message": "",
              "data": {
                "status": 1,
                "bucket": "fhnfile",
                "object": "6a3deacfce8589ecce5faca06a7153766c412b10",
                "callback": {"callback":"http://cb","callback_var":"{}"},
                "pick_code": "pc"
              }
            }
        """.trimIndent()
        val env = json.decodeFromString(UploadInitEnvelope.serializer(), src)

        assertEquals(true, env.state)
        assertEquals(1, env.data.status)
        assertEquals("fhnfile", env.data.bucket)
    }

    @Test fun `UploadInitReq serializes file_size as string not Long`() {
        // 协议契约:file_size 在 wire 上是 STRING(Go SDK strconv.FormatInt(fileSize, 10))
        val req = UploadInitReq(
            file_name = "test.bin",
            file_size = "1234567",   // 注意:String 不是 Long
            target = "U_1_0",
            fileid = "DEADBEEF",
            preid = "BEEFDEAD",
            pick_code = "",
            sign_key = "",
            sign_val = "",
        )

        val encoded = json.encodeToString(UploadInitReq.serializer(), req)

        // file_size 必须以字符串字面量出现,不是裸数字
        assertTrue("expected file_size to be encoded as string, got: $encoded",
            encoded.contains("\"file_size\":\"1234567\""))
        // topupload 不应出现(对齐 115 官方文档"必填:否"+ Go SDK 调用时不发此 key)
        assertTrue("topupload should not be encoded, got: $encoded",
            !encoded.contains("topupload"))
    }

    @Test fun `UploadInitReq fields are wire-format lower underscore`() {
        // 用非空默认值避开 kotlinx-serialization 默认值 omit 的副作用
        val req = UploadInitReq(
            file_name = "x.bin",
            file_size = "100",
            target = "U_1_0",
            fileid = "A",
            preid = "B",
            pick_code = "pc-non-empty",   // 故意非空,确保 encode 出来
            sign_key = "sk-non-empty",
            sign_val = "sv-non-empty",
        )
        val encoded = json.encodeToString(UploadInitReq.serializer(), req)

        // 关键字段名必须跟 Go SDK / 官方文档 1:1(下划线 / 全小写)
        assertTrue(encoded.contains("\"file_name\""))
        assertTrue(encoded.contains("\"file_size\""))
        assertTrue(encoded.contains("\"target\""))
        assertTrue(encoded.contains("\"fileid\""))
        assertTrue(encoded.contains("\"preid\""))
        assertTrue(encoded.contains("\"pick_code\""))
        assertTrue(encoded.contains("\"sign_key\""))
        assertTrue(encoded.contains("\"sign_val\""))
        // 绝对不能出现 camelCase 改写
        assertTrue("fileId camelCase leaked: $encoded", !encoded.contains("fileId"))
        assertTrue("pickCode camelCase leaked: $encoded", !encoded.contains("pickCode"))
    }

    @Test fun `UploadInitResp parses callback as object shape`() {
        val src = """
            {
              "status": 1,
              "sign_key": "",
              "sign_check": "",
              "file_id": "",
              "target": "U_1_0",
              "bucket": "test-bucket",
              "object": "test-object",
              "callback": {
                "callback": "http://callback.example.com",
                "callback_var": "{\"k\":\"v\"}"
              },
              "pick_code": "abc123"
            }
        """.trimIndent()

        val resp = json.decodeFromString(UploadInitResp.serializer(), src)

        assertEquals(1, resp.status)
        assertEquals("test-bucket", resp.bucket)
        assertEquals("test-object", resp.`object`)
        assertEquals("abc123", resp.pick_code)

        // callback 解析为 Single(对象形态)
        val callback = resp.callback
        assertTrue("expected Single callback, got: $callback", callback is UploadCallback.Single)
        val info = (callback as UploadCallback.Single).value
        assertEquals("http://callback.example.com", info.callback)
        assertEquals("{\"k\":\"v\"}", info.callback_var)
    }

    @Test fun `UploadInitResp parses callback as array shape`() {
        // 115-sdk-go 用 StructOrArray[T] 兼容两种形态 — Kotlin 端 sealed class 对应
        val src = """
            {
              "status": 1,
              "sign_key": "",
              "sign_check": "",
              "file_id": "",
              "target": "U_1_0",
              "bucket": "b",
              "object": "o",
              "callback": [
                {
                  "callback": "http://callback1.example.com",
                  "callback_var": "{\"k\":\"1\"}"
                },
                {
                  "callback": "http://callback2.example.com",
                  "callback_var": "{\"k\":\"2\"}"
                }
              ],
              "pick_code": "p"
            }
        """.trimIndent()

        val resp = json.decodeFromString(UploadInitResp.serializer(), src)

        val callback = resp.callback
        assertTrue("expected Multi callback, got: $callback", callback is UploadCallback.Multi)
        val items = (callback as UploadCallback.Multi).items
        assertEquals(2, items.size)
        assertEquals("http://callback1.example.com", items[0].callback)
        assertEquals("http://callback2.example.com", items[1].callback)
    }

    @Test fun `UploadInitResp status field carries through`() {
        val src = """
            {
              "status": 6,
              "sign_key": "K-1",
              "sign_check": "100-200",
              "file_id": "",
              "target": "U_1_0",
              "bucket": "",
              "object": "",
              "callback": {"callback":"","callback_var":""},
              "pick_code": ""
            }
        """.trimIndent()

        val resp = json.decodeFromString(UploadInitResp.serializer(), src)

        assertEquals(6, resp.status)
        assertEquals("K-1", resp.sign_key)
        assertEquals("100-200", resp.sign_check)
    }
}