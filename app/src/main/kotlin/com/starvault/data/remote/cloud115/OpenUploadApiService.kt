package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 115 proapi open 域上传端点(OAuth Bearer 鉴权)。
 *
 * 端点参考:
 *  - GET  /open/upload/get_token — 拿 Aliyun OSS STS 临时凭证(官方文档 kzacvzl0g7aiyyn4)
 *  - POST /open/upload/init     — 上传初始化 / 二次认证 / 秒传检测(官方文档 ul4mrauo5i2uza0q)
 *  - POST /open/upload/resume   — 断点续传(M3 才用,本接口先 stub)
 *
 * **字段命名严格按服务端契约**,Kotlin 字段名 + `@Field("...")` 注解双保险:
 *  - `fileid` / `preid` / `pick_code` / `sign_key` / `sign_val` 都是下划线 / 全小写
 *  - **不发送 `topupload`** — 115 官方文档 "必填:否";Go SDK 调用时也不发此 key;
 *    M2 单文件场景直接不发送,M4 文件夹上传时再加 `@Field("topupload") tu: String = "0"`。
 *  - `target` 已经是 `U_1_<cid>` 完整格式(由 [UploadInitClient] 拼接),
 *    Retrofit 不再加前缀。
 *
 * **401 自动 refresh**:所有请求都走 [Cloud115ApiClient.buildOkHttpClient] 链路,
 * [Token401Interceptor] 处理 99 / 401* / 40140123/124/126,本接口不重复实现。
 */
interface OpenUploadApiService {

    /**
     * 获取 Aliyun OSS STS 临时凭证。
     *
     * 响应字段见 [UploadGetTokenResp](服务端驼峰大小写敏感)。
     */
    @GET("open/upload/get_token")
    suspend fun getUploadToken(): Response<UploadGetTokenEnvelope>

    /**
     * 上传初始化调度。响应 status 字段决定后续分支:
     *  - 1: 非秒传,继续走 OSS 上传
     *  - 2: 秒传,M2 报"暂不支持秒传"
     *  - 6/7/8: 二次认证,需要用 `sign_check` 区间 SHA1 重发
     *
     * @param target 完整 `U_1_<cid>` 格式(由调用方拼好后传入)
     */
    @FormUrlEncoded
    @POST("open/upload/init")
    suspend fun initUpload(
        @Field("file_name") file_name: String,
        @Field("file_size") file_size: String,
        @Field("target") target: String,
        @Field("fileid") fileid: String,
        @Field("preid") preid: String,
        @Field("pick_code") pick_code: String,
        @Field("sign_key") sign_key: String,
        @Field("sign_val") sign_val: String,
    ): Response<UploadInitEnvelope>

    /**
     * 断点续传初始化(M3 才用,先 stub)。签名 + 字段名按 115 官方文档预留。
     *
     * @see <a href="https://www.yuque.com/115yun/open/tzvi9sbcg59msddz">断点续传文档</a>
     */
    @FormUrlEncoded
    @POST("open/upload/resume")
    suspend fun resumeUpload(
        @Field("file_size") file_size: String,
        @Field("target") target: String,
        @Field("fileid") fileid: String,
        @Field("pick_code") pick_code: String,
    ): Response<Unit>
}