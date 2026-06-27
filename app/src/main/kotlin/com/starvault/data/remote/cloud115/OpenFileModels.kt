package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 115 proapi open 域文件 DTO 集合。
 *
 * 字段定义参考:
 *  - 官方文档 https://www.yuque.com/115yun/open/(对应页 URL 在每个 DTO 注释里)
 *  - OpenListTeam/115-sdk-go 同款字段命名(Go SDK const.go 注释每行标 yuque 链接,交叉印证)
 *  - p115client/client.py 实测响应
 *
 * 不复用 webapi 的字段名(如 `n / pc / sha / ico`),统一用 proapi 的
 * (`file_name / pick_code / sha1 / file_type / file_id`),即使语义相同。
 *
 * 注:**文件列表** (/open/ufile/files + /open/ufile/search) 当前 shape 见下方字段
 */

// ─────────────────── /open/ufile/files + /open/ufile/search 响应(沿用 webapi 同款) ───────────────────

/**
 * GET /open/ufile/files 和 /open/ufile/search 实际响应(顶层就是数据,没有 `data` 包装壳)。
 *
 *  state 用 [JsonElement] 同时兼容 Boolean / Int(与 [ApiEnvelope.isOk] 一致语义)。
 *
 *  关键字段(实测):
 *  - state      : Boolean,true=业务成功
 *  - errNo      : Int,0=无错误
 *  - error      : String?,错误信息
 *  - count      : Int,**当前筛选下的总条数**(文件夹/文件分别计数)
 *  - data       : List<JsonElement>,每个 item 形态不同(folder vs file)
 *  - path       : 面包屑(首页 [{name: "根目录", cid: "0", ...}])
 *  - order      : 排序字段
 *  - is_asc     : 0/1
 *  - offset     : 当前 offset
 *  - limit      : 当前 limit
 *
 *  data 里的 item 字段(实测):
 *  - **文件夹** show_dir=1:
 *      cid (string), n (name), pc (pick code), fl (sub-file ids),
 *      t/te/tu/tp/to (timestamps), fc=0, e="", 没有 fid
 *  - **文件**   show_dir=0:
 *      fid (string), cid (parent), n (name), s (bytes), ico (ext like "mp4"),
 *      pc (pick code), sha, t/te/tu/tp/to (timestamps), fc (file category),
 *      play_long / audio_play_long (时长,秒), sta (status), sh (share flag)
 *
 *  判别 file vs folder:`"fid" in element` 即可(folder 没有 fid 字段)。
 *
 *  ⚠️ 顶层 `cid` 字段**不放在 DTO**:实测有时 int (0) 有时 string ("0"),kotlinx-serialization 严格
 *  模式会抛 "Expected quotation mark" 错;不影响业务(请求时已自带 cid 参数)。
 *
 *  为简化映射,每个 item 存为 [JsonElement],由调用方([FilesRepository] / FileItemParser)按需解析。
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
    val limit: Int = 0,
) {
    /** 业务成功:state=true (Boolean) 或 state=1 (Int)。 */
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/**
 * 解析后的统一文件/文件夹条目(**中间类型**,仅在 repository 内部使用)。
 *
 *  替代"按需解析 JsonElement"——把 folder 和 file 两种 shape 映射到一个结构,让 ViewModel
 *  不用关心 115 的怪 API。
 *
 *  - isFolder=true  → 走 cid(folder id);sizeBytes/duration/playLong=null
 *  - isFolder=false → 走 fid;sizeBytes/playLong 有值;ico 用作 type 推断
 */
@Serializable
data class ParsedFileItem(
    val id: String,           // file → fid, folder → cid
    val parentId: String,     // 父目录 cid
    val name: String,         // n
    val ico: String = "",     // 文件扩展名(folder 为空)
    val sizeBytes: Long = 0L, // s(文件才有;folder = 0)
    val mtimeSec: Long = 0L,  // tp(创建/修改时间 unix 秒)
    val pickCode: String = "",// pc(下载用)
    val isFolder: Boolean,
    val playLong: Int = 0,    // 时长(秒),audio/video 才有
    val sha1: String = "",    // 文件 sha1(folder 为空)
    val fileCategory: Int = 0,// fc,folder=0;文件 1=audio 2=video 3=image ...
    /**
     * 115 缩略图 URL(/open/ufile/files 响应 `u` 字段)。
     *  - 仅 IMAGE / VIDEO 类文件有值(folder 永远为空)
     *  - URL 含 115 签名 (`?s=...&t=...`),带 115 Bearer 直接 GET 即可(OkHttp 拦截器自动注入)
     *  - 路径格式:`thumb.115.com/thumb/{sha1 头 1/2/4/8 char 分段}/{sha1}_100`
     */
    val thumbnailUrl: String = "",
)

// ─────────────────── /open/folder/get_info 响应(单文件/夹 metadata) ───────────────────

/**
 * GET /open/folder/get_info 响应(OAuth Bearer 鉴权,Preview 入口拿 metadata)。
 *
 *  官方文档:https://www.yuque.com/115yun/open/rl8zrhe2nag21dfw
 *  OpenList SDK 同步参考:OpenListTeam/115-sdk-go/fs.go:140-186(GetFolderInfoResp)。
 *
 *  字段对照:
 *  - state          : boolean  业务成功标记
 *  - message        : string   异常信息,成功时为空
 *  - code           : number   异常码,成功时返回 0
 *  - data           : object   单个文件(夹)详情(**注意是单个 object,不是 array**)
 *    - file_id      : string   文件(夹)ID
 *    - parent_id    : string   父目录ID(在 paths[0] 里)
 *    - file_name    : string   文件名
 *    - pick_code    : string   文件提取码
 *    - sha1         : string   sha1 值
 *    - file_size    : string   文件大小(API 文档给 string,实测 server 给 number,统一 Long 接)
 *    - size_byte    : int      文件(夹)总大小字节数(2025-06-06 新增)
 *    - file_type    : string   文件类型(类似 webapi 的 ico)
 *    - file_category: string   文件属性;1:文件,0:文件夹(注意 string,不是 int)
 *    - play_long    : number   视频时长;-1:正在统计,其他数值为秒数
 *    - ptime        : string   上传时间
 *    - utime        : string   修改时间
 *    - paths        : object[] 文件(夹)所在路径
 *      - file_id    : number   父目录ID
 *      - file_name  : string   父目录名称
 *
 *  **与 webapi `/files/get_info` 的差异**:
 *  - 字段名大改:`n → file_name` / `pc → pick_code` / `sha → sha1` / `ico → file_type` / `te → utime`
 *  - webapi 返回 `data: [item]`(数组),proapi 返回 `data: object`(单个对象)
 *  - proapi 没有 `thumbnail_url` 字段(缩略图要走 listFiles 拿 `u` 字段,或 thumb.115.com 自行构造)
 */
@Serializable
data class OpenFolderInfoResponse(
    val state: Boolean? = null,
    val message: String? = null,
    val code: Int = 0,
    val data: OpenFolderInfoData? = null,
)

/** 单个文件(夹)详情(OpenFolderInfoResponse.data)。 */
@Serializable
data class OpenFolderInfoData(
    /** 文件(夹)ID。Preview 用此字段去 downurl 拿原图。 */
    @SerialName("file_id") val fileId: String = "",
    /** 文件名。 */
    @SerialName("file_name") val fileName: String = "",
    /** 文件提取码。DownURL / VideoPlay 用此字段。 */
    @SerialName("pick_code") val pickCode: String = "",
    /** sha1 值。 */
    val sha1: String = "",
    /** 文件大小(API 文档标 string,实测 number,统一 Long 接,parse 失败 fallback 0L)。 */
    @SerialName("file_size") val fileSize: Long = 0L,
    /** 文件(夹)总大小字节数(2025-06-06 新增的字段,优先用这个)。 */
    @SerialName("size_byte") val sizeByte: Long = 0L,
    /** 文件类型(类似 webapi 的 ico,如 "mp4"/"jpg")。 */
    @SerialName("file_type") val fileType: String = "",
    /** 文件属性;1:文件,0:文件夹(string!不是 int)。 */
    @SerialName("file_category") val fileCategory: String = "",
    /** 视频时长;-1:正在统计,其他数值为秒数。 */
    @SerialName("play_long") val playLong: Double = 0.0,
    /** 上传时间(API 文档标 string,实测 number 秒)。 */
    val ptime: String = "",
    /** 修改时间(API 文档标 string,实测 number 秒)。 */
    val utime: String = "",
    /** 是否星标;1:星标,0:取消。 */
    @SerialName("is_mark") val isMark: String = "",
    /** 文件(夹)最近打开时间(秒)。 */
    @SerialName("open_time") val openTime: Long = 0L,
    /** 文件(夹)所在路径(面包屑)。 */
    val paths: List<OpenFolderInfoPath> = emptyList(),
)

/** 文件(夹)路径节点(OpenFolderInfoData.paths 单项)。 */
@Serializable
data class OpenFolderInfoPath(
    /** 父目录ID(API 文档标 number,实测 server 给 string 也能 parse)。 */
    @SerialName("file_id") val fileId: String = "",
    /** 父目录名称。 */
    @SerialName("file_name") val fileName: String = "",
)

// ─────────────────── /open/ufile/downurl 响应(原图/文件直链) ───────────────────

/**
 * POST /open/ufile/downurl 响应(OAuth Bearer 鉴权,**必须传 UA**,否则签名失败)。
 *
 *  官方文档:https://www.yuque.com/115yun/open/um8whr91bxb5997o
 *  OpenList SDK 同步参考:OpenListTeam/115-sdk-go/fs.go:298-319(DownURLResp + DownURL)。
 *
 *  **特殊响应 shape**:`data` 是 `Map<file_id, item>`,key 是 file_id(string),value 是 item。
 *  OpenList driver 取 `resp[obj.GetID()]`(https://github.com/OpenListTeam/OpenList/blob/main/drivers/115_open/driver.go:152-155)。
 *
 *  字段对照(item):
 *  - file_name : string  文件名
 *  - file_size : number  文件大小
 *  - pick_code : string  文件提取码
 *  - sha1      : string  文件 sha1 值
 *  - url       : object
 *    - url     : string  **文件下载地址(对图片就是原图直链,Coil 直接 GET)**
 *
 *  流程(图片预览):
 *  1. fetchMetadata(fid) → OpenFolderInfoData{pickCode, fileId, ...}
 *  2. downloadUrl(pickCode, ua) → OpenDownUrlResponse.data[fileId].url.url
 *  3. Coil 拉图
 *
 *  UA 必须传:115 CDN 端按 UA 签发不同 URL,移动端 UA 才能拿到 CDN 直链。
 *  OpenList 的处理:用 `base.UserAgent`(全局常量);我们的处理:复用现有 Android UA。
 */
@Serializable
data class OpenDownUrlResponse(
    val state: Boolean? = null,
    val message: String? = null,
    val code: Int = 0,
    /** key = file_id(string),value = 单个文件信息。 */
    val data: Map<String, OpenDownUrlItem> = emptyMap(),
)

/** 单个文件直链信息(OpenDownUrlResponse.data 单项)。 */
@Serializable
data class OpenDownUrlItem(
    /** 文件名。 */
    @SerialName("file_name") val fileName: String = "",
    /** 文件大小(byte)。 */
    @SerialName("file_size") val fileSize: Long = 0L,
    /** 文件提取码(回传)。 */
    @SerialName("pick_code") val pickCode: String = "",
    /** 文件 sha1 值。 */
    val sha1: String = "",
    /** 文件下载地址(含 115 签名)。对图片 = 原图 URL;对视频 = mp4 直链(不是 m3u8)。 */
    val url: OpenDownUrlUrl = OpenDownUrlUrl(),
)

/** 嵌套的 url 容器(OpenDownUrlItem.url)。 */
@Serializable
data class OpenDownUrlUrl(
    /** 文件下载地址 URL。 */
    val url: String = "",
)

// ─────────────────── /open/video/play 响应(视频 m3u8 流) ───────────────────

/**
 * GET /open/video/play 响应(OAuth Bearer 鉴权,Preview 拿视频 m3u8)。
 *
 *  官方文档:https://www.yuque.com/115yun/open/hqglxv3cedi3p9dz
 *  OpenList SDK 同步参考:OpenListTeam/115-sdk-go/video.go:11-44(VideoPlayResp + VideoPlayURL)。
 *
 *  字段对照:
 *  - state      : bool    业务成功标记
 *  - message    : string  异常信息
 *  - code       : int     异常码
 *  - data       : object  视频信息
 *    - file_id  : string  文件 ID
 *    - file_name: string  文件名
 *    - file_size: int64   文件大小
 *    - duration : int64   视频时长(秒,注意 proapi 用 `duration`,**不是** webapi 的 `play_long`)
 *    - width    : int     视频宽度
 *    - height   : int     视频高度
 *    - video_url: array   **各清晰度的播放地址列表**(不是单个 string!)
 *      - url        : string  播放地址(m3u8)
 *      - definition : int     清晰度;1=标清 2=高清 3=超清 4=1080P 5=4k 100=原画
 *      - desc       : string  清晰度名称
 *    - multitrack_list     : array   多音轨列表(可选)
 *    - definition_list_new : array   所有可用清晰度列表(可选)
 *
 *  **与 webapi `/files/video` 的差异**:
 *  - webapi `data.video_url` 是单个 string;proapi `data.video_url` 是 array,选第 0 项即可
 *  - proapi 字段叫 `duration`,webapi 字段叫 `play_long`
 *  - proapi 没有 `thumbnail_url` 字段(用 Files 列表的 `u` 字段当缩略图)
 *
 *  MPV 选清晰度策略:取第一个 `video_url[0].url`(服务端按可用性排序,通常是 1080P)。
 */
@Serializable
data class OpenVideoPlayResponse(
    val state: Boolean? = null,
    val message: String? = null,
    val code: Int = 0,
    val data: OpenVideoPlayData? = null,
)

/** 视频播放信息(OpenVideoPlayResponse.data)。 */
@Serializable
data class OpenVideoPlayData(
    /** 文件 ID。 */
    @SerialName("file_id") val fileId: String = "",
    /** 文件名。 */
    @SerialName("file_name") val fileName: String = "",
    /** 文件大小(byte)。 */
    @SerialName("file_size") val fileSize: Long = 0L,
    /** 视频时长(秒)。 */
    val duration: Long = 0L,
    /** 视频宽度。 */
    val width: Int = 0,
    /** 视频高度。 */
    val height: Int = 0,
    /** 各清晰度播放地址列表。取 [videoUrl] 第一个 url 即可。 */
    @SerialName("video_url") val videoUrl: List<OpenVideoPlayUrl> = emptyList(),
    /** 多音轨列表(本次 MVP 不处理)。 */
    @SerialName("multitrack_list") val multitrackList: List<JsonElement> = emptyList(),
    /** 所有可用清晰度列表(本次 MVP 不处理)。 */
    @SerialName("definition_list_new") val definitionListNew: List<JsonElement> = emptyList(),
)

/** 单个清晰度播放地址(OpenVideoPlayData.videoUrl 单项)。 */
@Serializable
data class OpenVideoPlayUrl(
    /** 播放地址(m3u8)。 */
    val url: String = "",
    /** 清晰度;1=标清 2=高清 3=超清 4=1080P 5=4k 100=原画。 */
    val definition: Int = 0,
    /** 清晰度名称。 */
    val desc: String = "",
)

// ─────────────────── /open/folder/add 响应(新建文件夹) ───────────────────

/**
 * POST /open/folder/add 响应(OAuth Bearer 鉴权,Files 屏新建文件夹)。
 *
 *  官方文档:https://www.yuque.com/115yun/open/qur839kyx9cgxpxi
 *  OpenList SDK 同步参考:OpenListTeam/115-sdk-go/fs.go:12-25(MkdirResp)。
 *
 *  字段对照:
 *  - state   : bool    业务成功标记
 *  - message : string  异常信息(成功时为空)
 *  - code    : int     异常码(成功为 0)
 *  - data    : object  新建文件夹信息
 *    - file_name : string  创建的文件夹名(回显,可能含 115 端转码后的名字)
 *    - file_id   : string  新建文件夹 ID
 *
 *  ⚠️ 错误识别:115 在重名 / 路径非法 / 权限不足等场景下 state=false + message 非空。
 *  state 与 code 同时看,code 是 115 端标准错误码(对齐 401 拦截器的 40140123 等家族)。
 */
@Serializable
data class OpenFolderAddResponse(
    val state: Boolean? = null,
    val message: String? = null,
    val code: Int = 0,
    val data: OpenFolderAddData? = null,
)

/** 新建文件夹信息(OpenFolderAddResponse.data)。 */
@Serializable
data class OpenFolderAddData(
    /** 创建的文件夹名(可能含 115 端转义后的名字,带 `/` 等保留字符会替换)。 */
    @SerialName("file_name") val fileName: String = "",
    /** 新建文件夹 ID。 */
    @SerialName("file_id") val fileId: String = "",
)