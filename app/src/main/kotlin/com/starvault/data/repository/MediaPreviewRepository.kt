package com.starvault.data.repository

import com.starvault.data.remote.cloud115.FileApiService
import com.starvault.data.remote.cloud115.FileInfoResponse

/**
 * 媒体预览仓库：把 IMAGE / VIDEO 文件的 fid → 原图 URL / m3u8 URL 打通。
 *
 *  两个 115 端点（Cookie 鉴权）：
 *  - GET /files/get_info?file_id=xxx        → 拿 metadata（pickcode + name + size）
 *  - GET /files/download?pickcode=xxx       → 拿图片原图 file_url
 *  - GET /files/video?pickcode=xxx          → 拿视频 m3u8 video_url
 *
 *  流程（Image）：
 *  1. getInfo(fid)         → pickCode, name, sizeBytes
 *  2. downloadUrl(pickCode) → fileUrl（图片直链）
 *
 *  流程（Video）：
 *  1. getInfo(fid)         → pickCode, name, sizeBytes
 *  2. videoStream(pickCode) → videoUrl（m3u8 流）
 *
 *  错误策略：所有 HTTP / 业务失败 → Result.failure(message)；UI 屏展示 Error 分支。
 *
 *  设计说明：getInfo 复用 FilesRepository.listFolder 不划算（要传 cid 才知道文件在哪个目录），
 *  直接走专用的 /files/get_info。Preview 是一次性进入，不需要 listFolder 的分页/缓存机制。
 */
class MediaPreviewRepository(
    private val api: FileApiService,
) {
    /**
     * 拉单个文件的 metadata。
     *
     * @param fid 115 文件 id（从 FilesScreen FileEntry.id 拿到）
     * @return Result.success([MediaMetadata]) / Result.failure
     */
    suspend fun fetchMetadata(fid: String): Result<MediaMetadata> {
        return runCatching {
            val resp = api.getInfo(fileId = fid)
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
            val body = resp.body() ?: throw IllegalStateException("empty body")
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1} fid=$fid"
                throw IllegalStateException(msg)
            }
            body
        }.mapCatching { body ->
            val item = body.data.firstOrNull()
                ?: throw IllegalStateException("文件不存在或已删除")
            // file 项必须有 fid（folder 项没 fid）；folder 不会进 Preview 流程但兜底防御
            if (item.fid.isEmpty() && item.cid.isEmpty()) {
                throw IllegalStateException("文件不存在或已删除")
            }
            MediaMetadata(
                fid = item.fid.ifEmpty { item.cid },
                name = item.n,
                pickCode = item.pc,
                sizeBytes = item.s,
                ico = item.ico,
                sha1 = item.sha,
                thumbnailUrl = item.u,
                // 115 te 字段为 unix 秒；为 0 时 UI 展示"未知日期"
                mtimeSec = item.te,
            )
        }
    }

    /**
     * 拿图片原图 URL（直链，带 115 签名）。
     *
     * 用 `/files/image` 而不是 `/files/download`：实测 /files/download 对 PNG 小图等
     * `data` 返回空 array。p115client 注释也推荐图片走专用 image endpoint。
     *
     * @param pickCode 调 getInfo 拿到的 pc 字段
     * @return Result.success(url) / Result.failure
     */
    suspend fun fetchImageOriginalUrl(pickCode: String): Result<String> {
        return runCatching {
            val resp = api.imageOriginal(pickcode = pickCode)
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
            val body = resp.body() ?: throw IllegalStateException("empty body")
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1}"
                throw IllegalStateException(msg)
            }
            body
        }.mapCatching { body ->
            val url = body.data.url.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("无法获取原图 URL")
            url
        }
    }

    /**
     * 拿视频 m3u8 URL（带 115 签名，可直接给 Media3 播放）。
     *
     * @param pickCode 调 getInfo 拿到的 pc 字段
     * @return Result.success(url) / Result.failure
     */
    suspend fun fetchVideoM3u8Url(pickCode: String): Result<String> {
        return runCatching {
            val resp = api.videoStream(pickcode = pickCode)
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
            val body = resp.body() ?: throw IllegalStateException("empty body")
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1}"
                throw IllegalStateException(msg)
            }
            body
        }.mapCatching { body ->
            val url = body.data.videoUrl
            if (url.isBlank()) throw IllegalStateException("无法获取视频播放地址")
            url
        }
    }
}

/**
 * Preview 入口元数据（image / video 通用）。
 *
 * - fid         : 115 file id（也用作路由参数）
 * - name        : 文件名（顶部 AppBar 显示用）
 * - pickCode    : 调 download/video 端点用
 * - sizeBytes   : 文件大小（展示用，可选）
 * - ico         : 扩展名（mp4/jpg ...），备用
 * - sha1        : sha1，备用
 * - thumbnailUrl: 缩略图（可作 loading 占位图，本次 MVP 不用——直接显示 spinner）
 * - mtimeSec    : 115 文件修改时间 unix 秒（来自 /files/get_info 的 `te` 字段）；
 *                 UI 元数据栏展示用；为 0 表示服务端未返回
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
)