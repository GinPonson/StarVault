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
 *  注意 115 这套 /files API 的几个怪点：
 *  - 同一个端点必须用 `show_dir` 区分文件夹 vs 文件，两次都返回 state=true 但数据形态不同：
 *    show_dir=1 → 仅文件夹（每个 item 有 cid，但**无 fid**，fc=0）
 *    show_dir=0 → 仅文件  （每个 item 有 fid + s(字节) + ico(扩展名) + fc(file_category)）
 *  - 因此"我的文件"屏需并行 2 次请求合并结果（见 [FilesRepository]）
 *  - 文件 id 是 string（fid / cid），文件夹 id 也是 string，避免 toLong
 */
interface FileApiService {

    /**
     * GET /files — 列出某目录的子项（文件夹或文件，按 show_dir 区分）。
     *
     *  关键参数：
     *  - aid      : 应用 id，固定 1
     *  - cid      : 父目录 id，根目录为 "0"
     *  - o        : 排序字段，默认 `user_ptime`（修改时间）
     *  - asc      : 0=降序 1=升序
     *  - offset   : 分页偏移（默认 0）
     *  - limit    : 单页大小（默认 / 最大 50）
     *  - show_dir : 1=只文件夹, 0=只文件
     *
     *  响应：顶层 { state: Boolean, count: Int, data: [...] }，**没有 data 包装壳**，
     *  所以用独立的 [FileListResponse]（与 [com.starvault.data.remote.cloud115.SpaceSummuryResponse] 同样的处理）。
     *
     *  实测返回 size 上限 50（即使 limit=50，实际只回 48），分页用 offset+limit 累加。
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
    ): Response<FileListResponse>
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
)