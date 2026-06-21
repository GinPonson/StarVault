package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 115 webapi 文件域端点（base URL = webapi.115.com/）。
 *
 *  用途：Files 屏 "我的文件" 列表，按 folderId 拉子项。
 *  共用 [Cloud115ApiClient.buildOkHttpClient]（含 Cookie 注入 + 浏览器伪装头）。
 *
 *  所有端点都需登录态（Cookie 头由 [CookieInterceptor] 注入）。
 *
 *  注意 115 /files API 的关键参数（来自 p115client/tool/fs_files.py:221）：
 *  - show_dir=1（默认）+ 不带 nf → 115 一次性返回目录+文件混合数据
 *  - fc_mix  : 0=目录置顶, 1=按排序混合
 *  - nf=1   : 仅显示目录（与 show_dir=1 配合使用）
 *  - show_dir=0: 仅文件
 *  - 早期设计以为 show_dir=1 严格只返回目录，实际 115 在某些情况会带文件，
 *    [FilesRepository] 已用 distinctBy + 形状守卫防御，**不要**改回「并行两次请求」
 *    （那会拿到重复文件）。
 */
interface FileApiService {

    /**
     * GET /files — 列出某目录的子项（文件夹 + 文件 混合）。
     *
     *  调用方式：单次请求 `show_dir=1 + fc_mix=1`，115 在一次响应里返回目录 + 文件混合列表。
     *  数据形态区分：folder item 无 `fid` 字段，file item 有 `fid`（见 [ParsedFileItem.isFolder]）。
     *
     *  关键参数：
     *  - aid      : 应用 id，固定 1
     *  - cid      : 父目录 id，根目录为 "0"
     *  - o        : 排序字段，默认 `user_ptime`（修改时间）
     *  - asc      : 0=降序 1=升序
     *  - offset   : 分页偏移（默认 0）
     *  - limit    : 单页大小（默认 50，p115client 实测上限 1150）
     *  - show_dir : 固定 1（115 默认值，配合 fc_mix=1 取混合数据）
     *  - fc_mix   : 0=目录置顶，1=混合排序（默认 1）
     *  - nf       : 0/null=显示文件，1=仅显示目录（Files 屏用 0，根目录展示混合）
     *
     *  响应：顶层 { state, count, data, path, ... }（**没有 data 包装壳**），
     *  所以用独立的 [FileListResponse]（与 [com.starvault.data.remote.cloud115.SpaceSummuryResponse] 同样的处理）。
     */
    @GET("files")
    suspend fun listFiles(
        @Query("aid") aid: Int = 1,
        @Query("cid") cid: String,
        @Query("o") order: String = "user_ptime",
        @Query("asc") asc: Int = 0,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("show_dir") showDir: Int = 1,
        @Query("fc_mix") fcMix: Int = 1,
        @Query("nf") nf: Int? = null,
    ): Response<FileListResponse>

    /**
     * GET /files/download — 拿指定 pickcode 的真实下载 URL（用于图片原图 / 大文件）。
     *
     *  调用模式：单一文件 pickcode，115 响应 JSON 含 `data[0].file_url`，该 URL 可被
     *  Coil 直接拉图（已带 115 Cookie + 签名）。参考 p115client/client.py:7646。
     *
     *  参数：
     *  - pickcode : 提取码（115 `/files` 响应的 `pc` 字段）
     *  - dl       : 0=普通下载直链（默认）；1=需 302 二次跳转拿 file_url_302（单次有效）
     *
     *  ⚠️ 200 MB 限制（p115client 注释）：超此大小的文件此接口会失败，需用 m3u8 / 客户端下载。
     *
     *  响应：[FileDownloadResponse]
     *  - data[0].file_url     — 直链 URL（带 115 签名 + Cookie 已隐含在签名里）
     *  - data[0].file_name    — 文件名
     *  - data[0].file_size    — 字节数
     *  - data[0].file_sha1    — sha1
     */
    @GET("files/download")
    suspend fun downloadUrl(
        @Query("pickcode") pickcode: String,
        @Query("dl") dl: Int = 0,
    ): Response<FileDownloadResponse>

    /**
     * GET /files/image — 拿图片原图 URL（用于 PreviewImage）。
     *
     *  为什么不用 `/files/download`：
     *  - `/files/download` 是通用下载端点，对部分图片类型（PNG 小图等）实测 `data` 返回空 array
     *    （p115client 注释：image-only endpoint 走 /files/image）
     *  - `/files/image` 专给图片，返回 `data[].url` + `data[].size_url`（多尺寸）
     *
     *  调用模式：传 pickcode，115 返回原图 + 各尺寸 CDN URL（带签名）。
     *
     *  响应：[FileImageResponse]
     *  - data.url            — 原图 URL（CDN 带签名）
     *  - data.size_url.<sz>  — 指定尺寸的 URL（sz: small / middle / large 等）
     *
     *  参考 p115client/client.py:12893。
     *
     *  ⚠️ 上限 50 MB（p115client 注释）。
     */
    @GET("files/image")
    suspend fun imageOriginal(
        @Query("pickcode") pickcode: String,
    ): Response<FileImageResponse>

    /**
     * GET /files/video — 拿指定 pickcode 视频的 m3u8 在线播放地址（用于 PreviewVideo）。
     *
     *  响应：[VideoStreamResponse]
     *  - data.video_url       — m3u8 直链 URL（带 115 签名 + 域 cookie，可直接给 Media3 播放）
     *  - data.thumbnail_url   — 视频首帧缩略图
     *  - data.duration        — 视频时长（秒，p115client 注释实测可能有）
     *
     *  参考 p115client/client.py:16323。注意：未转码视频会触发自动转码，p115client 注释提到
     *  "如果返回信息中有 queue_url，则可用于查询转码状态"。我们本次 MVP 不实现轮询，
     *  UI 显示 buffering 状态由 Media3 自行处理。
     */
    @GET("files/video")
    suspend fun videoStream(
        @Query("pickcode") pickcode: String,
    ): Response<VideoStreamResponse>

    /**
     * GET /files/search — 115 全账号文件名搜索（substring 匹配）。
     *
     *  调用方式：传 search_value（关键词），115 返回与 listFiles 同结构（state/count/data/path/order/is_asc）的响应。
     *  data item shape 与 listFiles 一致（folder / file 混合），复用 [ParsedFileItem] 解析。
     *
     *  关键参数（参考 p115client/client.py:15425）：
     *  - search_value : 搜索关键词（**必填**，空字符串 115 返回空）
     *  - aid          : 应用 id，固定 1
     *  - cid          : 限定目录；本 MVP 不传 = 全账号搜
     *  - o / asc      : 排序字段 + 升降序（与 listFiles 同）
     *  - offset / limit : 分页
     *  - show_dir     : 固定 1（取混合数据）
     *  - fc_mix       : 固定 1（混合排序）
     *
     *  响应：[FileListResponse]（与 listFiles 同 DTO）
     *
     *  ⚠️ 115 不提供 type / size / mtime 等高级筛选，只能搜文件名（p115client 注释）。
     */
    @GET("files/search")
    suspend fun searchFiles(
        @Query("search_value") searchValue: String,
        @Query("aid") aid: Int = 1,
        @Query("cid") cid: String = "0",
        @Query("o") order: String = "user_ptime",
        @Query("asc") asc: Int = 0,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("show_dir") showDir: Int = 1,
        @Query("fc_mix") fcMix: Int = 1,
    ): Response<FileListResponse>

    /**
     * GET /files/get_info — 拿单个文件/目录的详细信息（含 pickcode + name + size + ico）。
     *
     *  用于 PreviewImage / PreviewVideo 第一步：拿到 pickcode 再去调 download / video endpoint。
     *  Files 屏列表已有 pickcode，但跨屏拿不到——只能靠 fid 再调一次。
     *
     *  参考 p115client/client.py:10253。注意：被移到回收站后此接口查不到，需还原/彻底删除。
     *
     *  响应：[FileInfoResponse]
     *  - data[0].fid / cid       — id（注意 folder 用 cid，file 用 fid；同 listFiles 判别）
     *  - data[0].n               — 文件名
     *  - data[0].pc              — pickcode
     *  - data[0].s               — 文件字节数（folder=0）
     *  - data[0].ico             — 扩展名（mp4 / jpg ...）
     *  - data[0].sha             — sha1
     *  - data[0].u               — 缩略图 URL（与 listFiles 同；image/video 才有）
     */
    @GET("files/get_info")
    suspend fun getInfo(
        @Query("file_id") fileId: String,
    ): Response<FileInfoResponse>
}

/**
 * GET /files 实际响应（顶层就是数据，没有 `data` 包装壳）。
 *
 *  state 用 [JsonElement] 同时兼容 Boolean / Int（与 [com.starvault.data.remote.cloud115.ApiEnvelope.isOk] 一致语义）。
 *
 *  关键字段（实测）：
 *  - state      : Boolean，true=业务成功
 *  - errNo      : Int，0=无错误
 *  - error      : String?，错误信息
 *  - count      : Int，**当前筛选下的总条数**（文件夹/文件分别计数）
 *  - data       : List<JsonElement>，每个 item 形态不同（folder vs file）
 *  - path       : 面包屑（首页 [{name: "根目录", cid: "0", ...}]）
 *  - order      : 排序字段
 *  - is_asc     : 0/1
 *  - offset     : 当前 offset
 *  - limit      : 当前 limit
 *
 *  data 里的 item 字段（实测）：
 *  - **文件夹** show_dir=1：
 *      cid (string), n (name), pc (pick code), fl (sub-file ids),
 *      t/te/tu/tp/to (timestamps), fc=0, e="", 没有 fid
 *  - **文件**   show_dir=0：
 *      fid (string), cid (parent), n (name), s (bytes), ico (ext like "mp4"),
 *      pc (pick code), sha, t/te/tu/tp/to (timestamps), fc (file category),
 *      play_long / audio_play_long (时长，秒), sta (status), sh (share flag)
 *
 *  判别 file vs folder：`"fid" in element` 即可（folder 没有 fid 字段）。
 *
 *  ⚠️ 顶层 `cid` 字段**不放在 DTO**：实测有时 int (0) 有时 string ("0")，kotlinx-serialization 严格
 *  模式会抛 "Expected quotation mark" 错；不影响业务（请求时已自带 cid 参数）。
 *
 *  为简化映射，每个 item 存为 [JsonElement]，由调用方（[FilesRepository] / [FileItemParser]）按需解析。
 */
@Serializable
data class FileListResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("errNo") val errNo: Int? = null,
    val count: Int = 0,
    val data: List<JsonElement> = emptyList(),
    val path: List<JsonElement> = emptyList(),
    val order: String = "user_ptime",
    val is_asc: Int = 0,
    val offset: Int = 0,
    val limit: Int = 50,
) {
    /** 业务成功：state=true (Boolean) 或 state=1 (Int)。 */
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/**
 * 解析后的统一文件/文件夹条目（**中间类型**，仅在 repository 内部使用）。
 *
 *  替代"按需解析 JsonElement"——把 folder 和 file 两种 shape 映射到一个结构，让 ViewModel
 *  不用关心 115 的怪 API。
 *
 *  - isFolder=true  → 走 cid（folder id）；sizeBytes/duration/playLong=null
 *  - isFolder=false → 走 fid；sizeBytes/playLong 有值；ico 用作 type 推断
 */
@Serializable
data class ParsedFileItem(
    val id: String,           // file → fid, folder → cid
    val parentId: String,     // 父目录 cid
    val name: String,         // n
    val ico: String = "",     // 文件扩展名（folder 为空）
    val sizeBytes: Long = 0L, // s（文件才有；folder = 0）
    val mtimeSec: Long = 0L,  // tp（创建/修改时间 unix 秒）
    val pickCode: String = "",// pc（下载用）
    val isFolder: Boolean,
    val playLong: Int = 0,    // 时长（秒），audio/video 才有
    val sha1: String = "",    // 文件 sha1（folder 为空）
    val fileCategory: Int = 0,// fc，folder=0；文件 1=audio 2=video 3=image ...
    /**
     * 115 缩略图 URL（webapi /files 响应 `u` 字段）。
     *  - 仅 IMAGE / VIDEO 类文件有值（folder 永远为空）
     *  - URL 含 115 签名 (`?s=...&t=...`)，带登录态 Cookie 直接 GET 即可
     *  - 路径格式：`thumb.115.com/thumb/{sha1 头 1/2/4/8 char 分段}/{sha1}_100`
     */
    val thumbnailUrl: String = "",
)