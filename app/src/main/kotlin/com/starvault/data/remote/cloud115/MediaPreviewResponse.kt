package com.starvault.data.remote.cloud115

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/* ─────────────────── /files/download 响应（图片原图 / 文件直链） ─────────────────── */

/**
 * 115 GET /files/download 响应（用于 PreviewImage 拿原图 URL）。
 *
 *  顶层 shape（p115client/client.py:7646 实测）：
 *  ```
 *  {
 *    "state": true,
 *    "data": [
 *      {
 *        "file_url":  "https://cdn-cf-workers.115.com/.../xxx.jpg?...",
 *        "file_name": "xxx.jpg",
 *        "file_size": 1234567,
 *        "file_sha1": "abc123...",
 *        "is_dir":    0
 *      }
 *    ]
 *  }
 *  ```
 *
 *  备注：
 *  - `data` 是 array（支持批量传 pickcode 拼接的逗号串，单个也是 1 元素数组）
 *  - `headers` 字段由 p115client 客户端代码 parse 时手动塞入（提取 Set-Cookie）。
 *    我们用 OkHttp 调此 endpoint 时，Cookie 已自动注入，所以不需要管 headers 字段——**忽略**。
 *  - 我们只取 `data[0].file_url`，所以 `data` 是 list 但 Repository 内部取第一个。
 */
@Serializable
data class FileDownloadResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("errNo") val errNo: Int? = null,
    val data: List<FileDownloadItem> = emptyList(),
) {
    /** 业务成功：state=true (Boolean) 或 state=1 (Int)。 */
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/** 单个文件的下载信息（位于 [FileDownloadResponse.data]）。 */
@Serializable
data class FileDownloadItem(
    /** 直链 URL（含 115 签名，Coil 直接 GET 可拉图） */
    @SerialName("file_url") val fileUrl: String = "",
    /** 文件名 */
    @SerialName("file_name") val fileName: String = "",
    /** 字节数 */
    @SerialName("file_size") val fileSize: Long = 0L,
    /** sha1 */
    @SerialName("file_sha1") val fileSha1: String = "",
    /** 是否文件夹；0=文件，1=文件夹（防御性） */
    @SerialName("is_dir") val isDir: Int = 0,
)

/* ─────────────────── /files/video 响应（视频 m3u8 流） ─────────────────── */

/**
 * 115 GET /files/video 响应（用于 PreviewVideo 拿 m3u8 URL）。
 *
 *  顶层 shape（p115client/client.py:16323 实测）：
 *  ```
 *  {
 *    "state": true,
 *    "data": {
 *      "video_url":     "https://cdn-cf-workers.115.com/.../xxx.m3u8?...",
 *      "thumbnail_url": "https://thumb.115.com/.../xxx.jpg",
 *      "duration":      1234,
 *      "queue_url":     "..."   // 仅未转码完成时存在（p115client 注释）
 *    }
 *  }
 *  ```
 *
 *  备注：
 *  - `data.video_url` 是 m3u8 主索引 URL（已签名，Media3 直接播放）
 *  - 我们用 `local=0` 默认（p115client 注释：local=1 在一些视频上不起作用）
 *  - duration / queue_url / multitrack_list 等字段 MVP 不处理，留作后续增强
 */
@Serializable
data class VideoStreamResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("errNo") val errNo: Int? = null,
    val data: VideoStreamData = VideoStreamData(),
) {
    /** 业务成功：state=true (Boolean) 或 state=1 (Int)。 */
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/** 视频播放信息（位于 [VideoStreamResponse.data]）。 */
@Serializable
data class VideoStreamData(
    /** m3u8 主索引 URL（带 115 签名） */
    @SerialName("video_url") val videoUrl: String = "",
    /** 视频首帧缩略图 URL（可选，本次 MVP 不用） */
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    /** 视频时长（秒，可选） */
    val duration: Int = 0,
    /** 转码队列 URL（未转码完成时存在，可用于轮询状态，MVP 忽略） */
    @SerialName("queue_url") val queueUrl: String = "",
)

/* ─────────────────── /files/image 响应（图片原图 URL） ─────────────────── */

/**
 * 115 GET /files/image 响应（用于 PreviewImage 拿原图 URL）。
 *
 *  顶层 shape（实测 webapi /files/image）：
 *  ```
 *  {
 *    "state": true,
 *    "data": {
 *      "url":      "https://thumb.115.com/.../xxx.png?...",  // 原图 URL（带签名）
 *      "width":    1920,
 *      "height":   1080,
 *      "size_url": { "small": "...", "middle": "...", "large": "..." },  // 多尺寸
 *      "blur":     0
 *    }
 *  }
 *  ```
 *
 *  备注：
 *  - **实测 `data` 是单个对象，不是 array**（p115client/client.py:12893 docstring 没说清楚）
 *  - 我们取 `data.url` 作为原图 URL（Coil 直接 GET）
 *  - 如果想用更小尺寸可走 `size_url.middle` 等，但 MVP 只用原图
 *  - 字段命名：实测是 `url`，不是 `image`（docstring 示例写错了）
 */
@Serializable
data class FileImageResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("errNo") val errNo: Int? = null,
    val data: FileImageData = FileImageData(),
) {
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/** 单个图片信息（位于 [FileImageResponse.data]）。 */
@Serializable
data class FileImageData(
    /** 原图 CDN URL（带 115 签名） */
    val url: String = "",
    /** 原图宽度 */
    val width: Int = 0,
    /** 原图高度 */
    val height: Int = 0,
    /** 多尺寸 URL 字典（可选字段） */
    @SerialName("size_url") val sizeUrl: Map<String, String> = emptyMap(),
    /** 0=非模糊图；1=原图被 115 替换成模糊图（防盗链） */
    val blur: Int = 0,
)

/* ─────────────────── /files/get_info 响应（单文件 metadata） ─────────────────── */

/**
 * 115 GET /files/get_info 响应（用于 Preview 入口拿 pickcode + name + size）。
 *
 *  顶层 shape（p115client/client.py:10253 实测）：
 *  ```
 *  {
 *    "state": true,
 *    "data": [
 *      { "fid": "abc", "cid": "...", "n": "xxx.jpg", "pc": "pickcode",
 *        "s": 12345, "ico": "jpg", "sha": "...", "u": "..." }
 *    ]
 *  }
 *  ```
 *
 *  data 是 array（即使 file_id 只有一个，115 也返回 1 元素数组）。
 *  item 既可能是 file shape（有 fid）也可能是 folder shape（有 cid 无 fid）——与 [FileListResponse]
 *  一致判别。本 MVP 只支持 IMAGE / VIDEO 文件型，folder 不会进 Preview 流程。
 */
@Serializable
data class FileInfoResponse(
    val state: JsonElement? = null,
    val error: String? = null,
    val errno: Int? = null,
    @SerialName("errNo") val errNo: Int? = null,
    val data: List<FileInfoItem> = emptyList(),
) {
    val isOk: Boolean
        get() {
            val s = state as? JsonPrimitive ?: return false
            return s.intOrNull?.let { it == 1 } ?: (s.booleanOrNull ?: false)
        }
}

/** 单个文件的 metadata（位于 [FileInfoResponse.data]）。 */
@Serializable
data class FileInfoItem(
    /** 文件 fid（folder 项无此字段） */
    val fid: String = "",
    /** 父目录 cid */
    val cid: String = "",
    /** 文件名 */
    val n: String = "",
    /** pickcode（调 download/video 端点用） */
    val pc: String = "",
    /** 字节数 */
    val s: Long = 0L,
    /** 扩展名小写 */
    val ico: String = "",
    /** sha1 */
    val sha: String = "",
    /** 缩略图 URL（image/video 才有，与 listFiles `u` 同） */
    val u: String = "",
    /** 文件修改时间 unix 秒（115 `/files/get_info` 响应字段，listFiles 也有；本次用于 UI 元数据栏） */
    val te: Long = 0L,
)