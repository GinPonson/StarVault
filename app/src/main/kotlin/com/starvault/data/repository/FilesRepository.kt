package com.starvault.data.repository

import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.FileApiService
import com.starvault.data.remote.cloud115.FileListResponse
import com.starvault.data.remote.cloud115.ParsedFileItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Files 列表仓库：调 115 webapi /files 端点，转成 [ParsedFileItem]。
 *
 *  调用模式：
 *  - listFolder(cid) → **单次**请求 `show_dir=1 + fc_mix=1`，由 115 一次性返回
 *    目录 + 文件的混合列表（按 fc_mix=1 排序）。这与 p115client 的
 *    `iter_fs_files` 默认 payload（p115client/tool/fs_files.py:221）一致。
 *
 *  分页：115 /files 支持 `offset` / `limit`（默认 50，p115client 实测上限 1150）。
 *  listFolder(cid, offset, limit) 拿单页；用 [PagedFiles.items] + [PagedFiles.hasMore]
 *  决定是否再翻页。totalCount 是 115 当前筛选下的总条数（用于 "共 N 项" 顶部展示）。
 *
 *  错误处理：
 *  - HTTP / 业务失败 → Result.failure，message 来自 115 error 字段
 *  - 成功 → 解析后 distinctBy({it.id}) 去重（兜底防御 115 偶发重复）
 *
 *  ⚠️ 115 /files 形状：
 *  - folder item：无 `fid` 字段，文件夹 id 用 `cid`
 *  - file item：有 `fid` + `s`(字节) + `ico`(扩展名) + `fc`(file_category) + `play_long`(秒)
 *  - 判别 `"fid" in element`（见 [parseRaw]）
 *
 *  历史教训：早期误以为 show_dir=1 严格只返回目录，于是并行 show_dir=1 + show_dir=0
 *  两次请求合并。实测 show_dir=1 偶尔也会带文件 → 合并后单文件出现两次（"021.png 看到 2 个" bug）。
 *  正确做法是单次请求让 115 自己混合返回，parseRaw 内部按 `fid` 字段判别 shape。
 */
class FilesRepository(
    private val api: FileApiService,
) {
    /**
     * 列出某目录的子项（单页）。
     *
     * @param cid   父目录 id；根目录传 "0"
     * @param offset  分页偏移（首屏 = 0）
     * @param limit   单页大小（默认 [DEFAULT_PAGE_SIZE] = 50）
     */
    suspend fun listFolder(
        cid: String,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): Result<PagedFiles> {
        return runCatching {
            val resp = api.listFiles(cid = cid, offset = offset, limit = limit, showDir = 1, fcMix = 1)
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
            val body = resp.body() ?: throw IllegalStateException("empty body")
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1} cid=$cid"
                throw IllegalStateException(msg)
            }
            body
        }.map { body ->
            val items = body.data.mapNotNull { parseRaw(it) }.distinctBy { it.id }
            PagedFiles(
                items = items,
                offset = body.offset,
                limit = body.limit,
                totalCount = body.count,
                hasMore = body.hasMore(),
            )
        }
    }

    companion object {
        /**
         * 默认单页大小 50。
         *
         *  115 /files 上限 1150（p115client 实测），但目录里典型是 50–500 项，
         *  一次拉 50 平衡：响应快、不占内存、用户滚到末尾再拉下一页。
         *  1.04 TB 媒体库才会需要调大。
         */
        const val DEFAULT_PAGE_SIZE = 50
    }

    /**
     * 把单个 JsonElement（folder 或 file shape）解析成 [ParsedFileItem]。
     *
     *  判别：`"fid" in obj` → file，否则 folder。
     *  - folder → id 用 `cid`，父目录用同 `cid`
     *  - file   → id 用 `fid`，父目录用 `cid`
     *
     *  解析失败 / 缺关键字段返回 null（被 mapNotNull 过滤，不影响其他 item）。
     */
    private fun parseRaw(element: JsonElement): ParsedFileItem? {
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return null
        val hasFid = "fid" in obj
        val name = obj.stringOrNull("n") ?: return null
        val parentCid = obj.stringOrNull("cid") ?: "0"
        val pc = obj.stringOrNull("pc") ?: ""

        return if (hasFid) {
            // ───── 文件 ─────
            val fid = obj.stringOrNull("fid") ?: return null
            val sizeBytes = obj.longOrZero("s")
            val ico = obj.stringOrNull("ico") ?: ""
            val fc = obj.intOrZero("fc")
            val playLong = obj.intOrZero("play_long")
            val sha1 = obj.stringOrNull("sha") ?: ""
            val mtime = obj.longOrZero("tp")
            // 115 webapi /files 响应 `u` 字段：图片/视频缩略图 URL（带 ?s=&t= 签名）
            // folder 无此字段；doc/zip 等类型也可能为空
            // URL 后缀反爬：
            //  - `_100`/`_250` 等数字后缀 → 17KB 固定占位图（115 反爬诱饵，**不要**用）
            //  - `_0` 或无后缀 → 真实原图；Coil 用 ContentScale.Crop 缩到 40dp 渲染
            val thumbnailUrl = (obj.stringOrNull("u") ?: "")
                .replace(Regex("_\\d+(?=\\?)"), "_0")
            ParsedFileItem(
                id = fid,
                parentId = parentCid,
                name = name,
                ico = ico,
                sizeBytes = sizeBytes,
                mtimeSec = mtime,
                pickCode = pc,
                isFolder = false,
                playLong = playLong,
                sha1 = sha1,
                fileCategory = fc,
                thumbnailUrl = thumbnailUrl,
            )
        } else {
            // ───── 文件夹 ─────
            val folderCid = obj.stringOrNull("cid") ?: return null
            val mtime = obj.longOrZero("tp")
            ParsedFileItem(
                id = folderCid,
                parentId = parentCid,
                name = name,
                pickCode = pc,
                mtimeSec = mtime,
                isFolder = true,
            )
        }
    }
}

/* ─────────────────── 扩展：根据 ico/扩展名映射 FileType ─────────────────── */

/**
 * 把 115 `ico` 字段（扩展名小写，如 "mp4"、"pdf"）映射到 [FileType]。
 *
 *  注意：
 *  - 115 `ico` 不一定带点；已是小写
 *  - 这里用纯字符串比较，避免依赖完整 mime 库
 *  - 命中不上时退回 OTHER
 */
fun ParsedFileItem.toFileType(): FileType {
    if (isFolder) return FileType.FOLDER
    val ext = ico.lowercase().trim()
    return when (ext) {
        in VIDEO_EXTS -> FileType.VIDEO
        in IMAGE_EXTS -> FileType.IMAGE
        in AUDIO_EXTS -> FileType.AUDIO
        in DOC_EXTS   -> FileType.DOC
        in ZIP_EXTS   -> FileType.ZIP
        else          -> FileType.OTHER
    }
}

private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "rmvb", "rm")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "tiff", "tif")
private val AUDIO_EXTS = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus", "wma", "ape")
private val DOC_EXTS   = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "rtf", "epub", "csv")
private val ZIP_EXTS   = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "dmg")

/* ─────────────────── JsonObject 扩展 helpers ─────────────────── */

private fun kotlinx.serialization.json.JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

private fun kotlinx.serialization.json.JsonObject.longOrZero(key: String): Long {
    val p = this[key] as? JsonPrimitive ?: return 0L
    // 115 返回的 size/tp 都是字符串型数字（"45503418"），少数可能是 Double
    p.longOrNullSafe()?.let { return it }
    p.doubleOrNull?.toLong()?.let { return it }
    p.intOrNull?.toLong()?.let { return it }
    return 0L
}

private fun kotlinx.serialization.json.JsonObject.intOrZero(key: String): Int {
    val p = this[key] as? JsonPrimitive ?: return 0
    p.intOrNull?.let { return it }
    p.longOrNullSafe()?.let { return it.toInt() }
    return 0
}

private fun JsonPrimitive.longOrNullSafe(): Long? {
    // JsonPrimitive 没有 longOrNull，自己 parse content
    val s = contentOrNull ?: return null
    return s.toLongOrNull()
}

/* ─────────────────── 分页结果 ─────────────────── */

/**
 * 115 /files 单页结果。
 *
 *  - items      : 本页解析后的条目
 *  - offset     : 本页 offset（来自响应，便于 log/debug）
 *  - limit      : 本页 limit（同上）
 *  - totalCount : 115 当前筛选下的总条数（**不可信**，仅参考；UI 不要直接显示）
 *  - hasMore    : 是否还有下一页
 *
 *  hasMore 判别（[FileListResponse.hasMore]）：
 *  1) `data.size < limit` → 短页即认为到底
 *  2) `offset + data.size >= count` → 严格按 115 总数
 *  3) 否则按"满页"假设有更多
 *
 *  totalCount 不可信原因：115 在某些目录会返回远超本目录实际子项的数（疑似全账号汇总
 *  或缓存滞后），用作"共 N 项"展示会让数字显示到上万。UI 应优先用 `items.size`。
 */
data class PagedFiles(
    val items: List<ParsedFileItem>,
    val offset: Int,
    val limit: Int,
    val totalCount: Int,
    val hasMore: Boolean,
) {
    /** 下一页的 offset；没有更多页时返回 null。 */
    val nextOffset: Int? get() = if (hasMore) offset + items.size else null
}

/**
 * 判别是否还有下一页。
 *
 *  115 `count` 字段在某些目录会返回远超本目录实际子项的数（疑似全账号汇总或缓存滞后），
 *  不可信；只用 count 会让空页循环触发。
 *
 *  判别顺序（满足任一即认为"没更多"）：
 *  1) 本页 < limit  → 115 已经返回短页，**肯定到底**
 *  2) (offset+size) ≥ count（count > 0 时）→ 严格按 115 报的总数
 *  3) 都不满足 → 保守认为"可能更多"
 */
private fun FileListResponse.hasMore(): Boolean {
    if (data.size < limit) return false
    if (count > 0) {
        val fetched = offset + data.size
        if (fetched >= count) return false
    }
    return true
}