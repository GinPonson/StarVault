package com.starvault.data.remote.cloud115

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * 115 proapi open 域上传 DTO 集合。
 *
 * 字段定义参考:
 *  - 官方文档 https://www.yuque.com/115yun/open/ul4mrauo5i2uza0q(文件上传)
 *  - 官方文档 https://www.yuque.com/115yun/open/kzacvzl0g7aiyyn4(获取上传凭证)
 *  - OpenListTeam/115-sdk-go/upload.go(Go SDK,与官方文档同源)
 *  - OpenListTeam/OpenList/drivers/115_open/upload.go(实际编排参考)
 *
 * 与 OpenFileModels.kt 同层,但放独立文件:
 *  - 上传是 M2 全新链路,跟下载 / 文件列表读取的关注点不一样
 *  - 文件多,保持单文件 < 200 行,便于 review
 *
 * ## 字段命名严格按服务端契约
 *
 *  错误示例:把 `fileid` 改成 `fileId`(camelCase)是错的——服务端区分大小写。
 *  正确做法:用 [kotlinx.serialization.SerialName] 映射,保持 Kotlin 字段 idiomatic。
 *
 * ## `topupload` 字段 M2 不发送
 *
 *  官方文档标注"必填:否",Go SDK 调用时也不发此 key。M2 单文件场景直接不发送,
 *  M4 文件夹上传时再加 `@Field("topupload") tu: String = "0"`。
 *  DTO 里不放此字段,JSON 序列化时自然不会写出。
 */

// ─────────────────── /open/upload/get_token 响应 ───────────────────

/**
 * GET /open/upload/get_token 顶层 envelope。
 *
 * 与 [UploadInitEnvelope] 同构 — 115 proapi 所有端点统一 `{state, code, message, data}` 4 字段。
 *
 * 字段对照:
 *  - state   : boolean  true=成功
 *  - code    : int     错误码(0 = 成功)
 *  - message : string  错误消息
 *  - data    : object  STS 凭证负载(见 [UploadGetTokenResp])
 */
@Serializable
data class UploadGetTokenEnvelope(
    val state: Boolean = false,
    val code: Int = 0,
    val message: String = "",
    val data: UploadGetTokenResp = UploadGetTokenResp(),
)

/**
 * GET /open/upload/get_token 业务负载(OAuth Bearer 鉴权)。
 *
 * 官方文档:https://www.yuque.com/115yun/open/kzacvzl0g7aiyyn4
 *
 * 返回 Aliyun OSS 的 STS 临时凭证 + endpoint。客户端拿这些凭证直接 PUT 到 OSS,
 * **不**复用 115 Bearer token。
 *
 * 字段对照:
 *  - endpoint        : OSS endpoint,例 `https://oss-cn-shanghai.aliyuncs.com`
 *  - AccessKeyId     : STS AK(注意驼峰,服务端区分大小写)
 *  - AccessKeySecret : STS SK(注意驼峰)
 *  - SecurityToken   : STS token(注意驼峰,不是 snake_case `security_token`)
 *  - Expiration      : ISO 8601 过期时间字符串(注意大写 E,与 SecurityToken 一致)
 *
 * DTO 字段名严格按服务端大小写,不用 [SerialName] 重命名——保持契约直观。
 */
@Serializable
data class UploadGetTokenResp(
    val endpoint: String = "",
    val AccessKeyId: String = "",
    val AccessKeySecret: String = "",
    val SecurityToken: String = "",
    val Expiration: String = "",
)

// ─────────────────── /open/upload/init 请求(form-urlencoded) ───────────────────

/**
 * POST /open/upload/init 请求体 — form-urlencoded,**不**是 JSON。
 *
 * 官方文档:https://www.yuque.com/115yun/open/ul4mrauo5i2uza0q
 *
 * 字段对照:
 *  - file_name : string  文件名(必填)
 *  - file_size : string  文件大小字节,**服务端是 string 不是 Long**(必填)
 *  - target    : string  目标目录,固定 `U_1_<cid>` 格式(必填)
 *  - fileid    : string  整文件 SHA1 十六进制大写(必填)
 *  - preid     : string  前 128KB SHA1 十六进制大写(必填)
 *  - pick_code : string  提取码;首次上传填空字符串(非必填)
 *  - sign_key  : string  二次认证 key(非必填,首次上传填空)
 *  - sign_val  : string  二次认证 value,大写 SHA1(非必填,首次上传填空)
 *
 * Retrofit 通过 `@Field("...")` 在接口层映射字段名;DTO 字段名跟服务端保持一致,
 * 这样 JSON encode(单元测试用)与 form encode(production)同源,不会出现 drift。
 */
@Serializable
data class UploadInitReq(
    @kotlinx.serialization.SerialName("file_name") val file_name: String = "",
    @kotlinx.serialization.SerialName("file_size") val file_size: String = "",
    val target: String = "",
    val fileid: String = "",
    val preid: String = "",
    @kotlinx.serialization.SerialName("pick_code") val pick_code: String = "",
    @kotlinx.serialization.SerialName("sign_key") val sign_key: String = "",
    @kotlinx.serialization.SerialName("sign_val") val sign_val: String = "",
)

// ─────────────────── /open/upload/init 响应 ───────────────────

/**
 * POST /open/upload/init 顶层 envelope。
 *
 * 115 proapi 端点统一返回 `{state, code, message, data: {...}}` 4 字段结构;
 * 业务字段都在 `data` 里(对齐 OpenList 115-sdk-go 与官方文档)。
 *
 * Retrofit 反序列化时把整个 envelope 读为 [UploadInitEnvelope],
 * 调用方 [UploadInitClient.init] 取 `.data` 后传给 [UploadStateMachine]。
 *
 * 字段对照:
 *  - state   : boolean  true=业务成功 / false=业务失败(由调用方进一步判断)
 *  - code    : int     错误码(0 = 成功);非 0 时 message 必有内容
 *  - message : string  人类可读错误消息
 *  - data    : object  业务负载(见 [UploadInitResp])
 */
@Serializable
data class UploadInitEnvelope(
    val state: Boolean = false,
    val code: Int = 0,
    val message: String = "",
    val data: UploadInitResp = UploadInitResp(),
)

/**
 * POST /open/upload/init 业务负载(envelope.data)。
 *
 * 字段对照(全部字段部分有条件):
 *  - status     : int  关键控制流,见下方语义表
 *  - sign_key   : string  二次认证 key(只在 status ∈ {6,7,8} 时有意义)
 *  - sign_check : string  二次认证区间,格式 "start-end"(只在 status ∈ {6,7,8} 时有意义)
 *  - file_id    : string  服务端文件 ID,秒传时已分配;真正上传完成后才有
 *  - target     : string  服务端的目标目录 CID(以响应为准)
 *  - bucket     : string  OSS bucket,真正上传时用
 *  - object     : string  OSS object key,真正上传时用
 *  - callback   : object | array<object>  OSS 回调信息(见 [UploadCallback] sealed class)
 *  - pick_code  : string  提取码;秒传 / 真正上传都返回
 */
@Serializable
data class UploadInitResp(
    val status: Int = 0,
    @kotlinx.serialization.SerialName("sign_key") val sign_key: String = "",
    @kotlinx.serialization.SerialName("sign_check") val sign_check: String = "",
    @kotlinx.serialization.SerialName("file_id") val file_id: String = "",
    val target: String = "",
    val bucket: String = "",
    /** OSS object key。`object` 是 Kotlin 关键字,Kotlin 字段名用 backtick,wire 格式用 `@SerialName("object")`。 */
    @kotlinx.serialization.SerialName("object") val `object`: String = "",
    /**
     * OSS 回调。服务端可能返回 object `{callback, callback_var}` 或
     * array `[{callback, callback_var}, ...]` 两种形态(Kotlin sealed 兼容)。
     */
    val callback: UploadCallback = UploadCallback.Single(CallbackInfo()),
    @kotlinx.serialization.SerialName("pick_code") val pick_code: String = "",
) {
    // 真实实现见下方分割的 `object` 字段被替换成 backing property。
}

// ─────────────────── Callback: sealed class 兼容 object / array ───────────────────

/**
 * OSS 上传回调信息。服务端实际只回 Single(对象),但 DTO 防御性兼容两种形态,
 * 对齐 Go SDK `115-sdk-go/json_types/StructOrArray[T]` 泛型设计。
 */
@Serializable(with = UploadCallbackSerializer::class)
sealed class UploadCallback {
    /** 单个对象形态:`{"callback": "...", "callback_var": "..."}` */
    data class Single(val value: CallbackInfo) : UploadCallback()

    /** 数组形态:`[{...}, {...}]` */
    data class Multi(val items: List<CallbackInfo>) : UploadCallback()
}

/** 回调内容(UploadCallback 内部项)。 */
@Serializable
data class CallbackInfo(
    val callback: String = "",
    @kotlinx.serialization.SerialName("callback_var") val callback_var: String = "",
)

/**
 * 自定义 [UploadCallback] 序列化器:根据 JSON 是 object 还是 array 自动选形态。
 *
 * 实现方式:读为 [JsonElement],看 `isArray` 还是 `JsonObject`,
 * 再委托给对应形态的 serializer。
 *
 * 为什么不用 kotlinx-serialization 的 polymorphic(`@SerialName` 路由)——
 * 服务端不会在 JSON 里加 `"type": "single" / "multi"` 这种判别字段,
 * 只能根据 JSON 顶层是不是 array 来区分。
 */
object UploadCallbackSerializer : KSerializer<UploadCallback> {
    @OptIn(
        kotlinx.serialization.InternalSerializationApi::class,
        kotlinx.serialization.ExperimentalSerializationApi::class,
    )
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("UploadCallback", SerialKind.CONTEXTUAL) {
            element<JsonElement>("payload", isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: UploadCallback) {
        // 只支持 JSON encode(单元测试用);production 是 form-urlencoded,不经过这里。
        require(encoder is JsonEncoder) { "UploadCallbackSerializer only supports JSON" }
        val json = when (value) {
            is UploadCallback.Single -> buildJsonObject {
                put("callback", JsonPrimitive(value.value.callback))
                put("callback_var", JsonPrimitive(value.value.callback_var))
            }
            is UploadCallback.Multi -> buildJsonArray {
                value.items.forEach { info ->
                    add(buildJsonObject {
                        put("callback", JsonPrimitive(info.callback))
                        put("callback_var", JsonPrimitive(info.callback_var))
                    })
                }
            }
        }
        encoder.encodeJsonElement(json)
    }

    override fun deserialize(decoder: Decoder): UploadCallback {
        require(decoder is JsonDecoder) { "UploadCallbackSerializer only supports JSON" }
        val element = decoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> UploadCallback.Multi(
                items = element.map { item ->
                    val obj = item as? JsonObject
                        ?: error("callback array item must be object, got: $item")
                    CallbackInfo(
                        callback = obj["callback"]?.jsonPrimitive?.contentOrEmpty() ?: "",
                        callback_var = obj["callback_var"]?.jsonPrimitive?.contentOrEmpty() ?: "",
                    )
                }
            )
            is JsonObject -> UploadCallback.Single(
                value = CallbackInfo(
                    callback = element["callback"]?.jsonPrimitive?.contentOrEmpty() ?: "",
                    callback_var = element["callback_var"]?.jsonPrimitive?.contentOrEmpty() ?: "",
                )
            )
            else -> error("callback must be object or array, got: $element")
        }
    }
}

/** 便利:从 [JsonPrimitive] 安全取字符串(空值兜底)。 */
private val JsonElement?.jsonPrimitive get() = this as? JsonPrimitive

private fun JsonPrimitive.contentOrEmpty(): String =
    runCatching { content }.getOrDefault("")