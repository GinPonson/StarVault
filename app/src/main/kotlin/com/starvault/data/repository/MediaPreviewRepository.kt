package com.starvault.data.repository

import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.requireSuccessful

/**
 * 媒体预览仓库:把 IMAGE / VIDEO 文件的 fid → 原图 URL / m3u8 URL 打通。
 *
 *  三个 115 Open Platform 端点(proapi.115.com + OAuth Bearer 鉴权):
 *  - GET  /open/folder/get_info?file_id=xxx    → 拿 metadata(file_id + pick_code + name + sha1 + size)
 *  - POST /open/ufile/downurl pick_code=xxx    → 拿图片原图 file_url(必须传 UA,见 OpenFileApiService.downloadUrl)
 *  - GET  /open/video/play?pick_code=xxx       → 拿视频 m3u8 video_url(array,选第一项)
 *
 *  流程(Image):
 *  1. getInfo(fid)         → pickCode, fileId, name, sizeBytes
 *  2. downloadUrl(pickCode) → resp.data[fileId].url.url(图片直链)
 *
 *  流程(Video):
 *  1. getInfo(fid)             → pickCode, name, sizeBytes
 *  2. videoPlay(pickCode)      → resp.data.videoUrl[].url + desc(m3u8 列表)
 *
 *  错误策略:所有 HTTP / 业务失败 → Result.failure(message);UI 屏展示 Error 分支。
 *
 *  设计说明:getInfo 复用 FilesRepository.listFolder 不划算(要传 cid 才知道文件在哪个目录),
 *  直接走专用的 /open/folder/get_info。Preview 是一次性进入,不需要 listFolder 的分页/缓存机制。
 *
 *  字段映射(proapi vs webapi):
 *  - `file_name` 代替 `n`
 *  - `pick_code` 代替 `pc`
 *  - `sha1`      代替 `sha`
 *  - `file_size` 代替 `s`
 *  - `file_type` 代替 `ico`(语义一致)
 *  - `utime`     代替 `te`
 *  - `play_long` (double) → `duration` (long,proapi 用 duration 字段)
 *
 *  参考:OpenListTeam/115-sdk-go/fs.go:GetFolderInfo + DownURL;video.go:VideoPlay。
 */
class MediaPreviewRepository(
    private val api: OpenFileApiService,
) {
    /**
     * 拉单个文件的 metadata。
     *
     * @param fid 115 文件 id(从 FilesScreen FileEntry.id 拿到)
     * @return Result.success([MediaMetadata]) / Result.failure
     */
    suspend fun fetchMetadata(fid: String): Result<MediaMetadata> {
        return runCatching {
            val body = api.getInfo(fileId = fid).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "code=${body.code} fid=$fid")
            }
            val data = body.data ?: throw IllegalStateException("文件不存在或已删除")
            // 文件必须有 file_id;folder 走 file_category="0",但 Preview 不会进 folder,这里是兜底
            if (data.fileId.isEmpty()) {
                throw IllegalStateException("文件不存在或已删除")
            }
            data
        }.map { data ->
            // file_category: "1" = 文件,"0" = 文件夹(string 类型,不是 int)
            // mtimeSec 优先用 utime(server 给 string,parse 失败 fallback 0L)
            val mtimeSec = data.utime.toLongOrNull() ?: 0L
            // sizeBytes 优先用 size_byte(2025-06-06 新增字段,字节数 Long),
            // fallback file_size(可能 string 也可能 number,已经按 Long parse)
            val sizeBytes = if (data.sizeByte > 0L) data.sizeByte else data.fileSize
            MediaMetadata(
                fid = data.fileId,
                name = data.fileName,
                pickCode = data.pickCode,
                sizeBytes = sizeBytes,
                ico = data.fileType,
                sha1 = data.sha1,
                thumbnailUrl = "",  // proapi get_info 不返回 thumbnailUrl;走 Files 列表的 `u` 字段
                mtimeSec = mtimeSec,
                fileCategory = data.fileCategory,
            )
        }
    }

    /**
     * 拿图片原图 URL(直链,带 115 签名)。
     *
     * 用 `/open/ufile/downurl` 而不是 `/files/image`(webapi 已废弃)。
     *
     * **流程**:
     *  1. 调 [fetchMetadata] 拿到 fileId + pickCode
     *  2. 调 downurl,响应是 `Map<file_id, item>`
     *  3. 取 `resp.data[fileId].url.url`(跟 OpenList driver 一致:resp[obj.GetID()])
     *
     * @param fileId 115 文件 id(用于从 downurl 响应的 Map 里精确取值,跟 OpenList 同步)
     * @param pickCode 调 getInfo 拿到的 pick_code 字段
     * @return Result.success(url) / Result.failure
     */
    suspend fun fetchImageOriginalUrl(fileId: String, pickCode: String): Result<String> {
        return runCatching {
            val body = api.downloadUrl(pickCode = pickCode).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "code=${body.code} pickCode=$pickCode")
            }
            body
        }.mapCatching { body ->
            // 优先按 fileId 精确取(跟 OpenList 一致);取不到就 fallback 第一个 entry
            val item = body.data[fileId]
                ?: body.data.values.firstOrNull()
                ?: throw IllegalStateException("无法获取原图 URL:downurl 返回空")
            val url = item.url.url.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("无法获取原图 URL")
            url
        }
    }

    /**
     * 拿音频直链(mp3/flac/wav 等,带 115 5min 签名,给 Media3 ProgressiveMediaSource 播放)。
     *
     * **流程**:
     *  1. 调 [fetchMetadata] 拿到 fileId + pickCode
     *  2. 调 downurl,响应是 `Map<file_id, item>`,跟 fetchImageOriginalUrl 1:1 同模式
     *  3. 取 `resp.data[fileId].url.url`
     *
     * 跟 fetchImageOriginalUrl 几乎 1:1 复制 — 唯一区别是 KDoc 描述("音频" / "mp3/flac/wav")。
     * 115 downurl 对所有"非视频"文件(图片/音频/文档)都返回直链 URL,签发规则统一。
     *
     * @param fileId 115 文件 id(用于从 downurl 响应的 Map 里精确取值,跟 OpenList 一致)
     * @param pickCode 调 getInfo 拿到的 pick_code 字段
     * @return Result.success(url) / Result.failure
     */
    suspend fun fetchAudioStreamUrl(fileId: String, pickCode: String): Result<String> {
        return runCatching {
            val body = api.downloadUrl(pickCode = pickCode).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "code=${body.code} pickCode=$pickCode")
            }
            body
        }.mapCatching { body ->
            // 优先按 fileId 精确取(跟 OpenList 一致);取不到就 fallback 第一个 entry
            val item = body.data[fileId]
                ?: body.data.values.firstOrNull()
                ?: throw IllegalStateException("无法获取音频 URL:downurl 返回空")
            val url = item.url.url.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("无法获取音频 URL")
            url
        }
    }

    /**
     * 拿视频全部可用 m3u8 列表(带 115 签名,可直接给 Media3 播放)。
     *
     * **字段差异**:
     *  - webapi `data.video_url` 是 string;proapi `data.video_url` 是 array
     *  - array 每一项 = 一个清晰度(1080P / 720P / 4K / 原画),服务端按可用性降序
     *  - 给 PreviewVideoScreen 画质切换用:列表展示,选哪条就 prepare 哪条的 url
     *
     * @param pickCode 调 getInfo 拿到的 pick_code 字段
     * @return Result.success([VideoM3u8] 列表) / Result.failure
     */
    suspend fun fetchVideoM3u8Options(pickCode: String): Result<List<VideoM3u8>> {
        return runCatching {
            val body = api.videoPlay(pickCode = pickCode).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "code=${body.code} pickCode=$pickCode")
            }
            body
        }.mapCatching { body ->
            val data = body.data ?: throw IllegalStateException("无法获取视频播放地址")
            val options = data.videoUrl
                .filter { it.url.isNotBlank() }
                .map { VideoM3u8(url = it.url, qualityDesc = it.desc) }
            if (options.isEmpty()) throw IllegalStateException("无法获取视频播放地址")
            options
        }
    }
}

/**
 * /open/video/play 响应产物(m3u8 URL + 清晰度名)。
 *
 *  - [url]         : 115 签名 m3u8,直接给 ExoPlayer.prepare 即可
 *  - [qualityDesc] : 115 给的清晰度名称,如 "1080P"/"720P"/"4K"/"原画",给 PreviewVideoScreen
 *                    顶栏右上角的画质 chip 显示;空字符串表示服务端没给,UI fallback "—"
 */
data class VideoM3u8(
    val url: String,
    val qualityDesc: String,
)

/**
 * Preview 入口元数据(image / video 通用)。
 *
 * - fid          : 115 file id(也用作路由参数)
 * - name         : 文件名(顶部 AppBar 显示用)
 * - pickCode     : 调 downurl / videoPlay 端点用
 * - sizeBytes    : 文件大小(展示用,可选)
 * - ico          : 扩展名(mp4/jpg ...),备用
 * - sha1         : sha1,备用
 * - thumbnailUrl: 缩略图(可作 loading 占位图,MVP 不用——直接显示 spinner)
 * - mtimeSec     : 115 文件修改时间 unix 秒(来自 /open/folder/get_info 的 `utime` 字段)
 * - fileCategory : "1"=文件 / "0"=文件夹(string,跟 webapi 的 Int 0/1 不同)
 */
data class MediaMetadata(
    val fid: String,
    val name: String,
    val pickCode: String,
    val sizeBytes: Long,
    val ico: String = "",
    val sha1: String = "",
    val thumbnailUrl: String = "",
    val mtimeSec: Long = 0L,
    /** proapi 用 string 表示文件/文件夹(1/0),不是 Int。 */
    val fileCategory: String = "1",
)