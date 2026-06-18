package com.starvault.data.repository

import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.FileApiService
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
     * 列出某目录的所有直接子项（文件夹 + 文件 混合）。
     *
     * @param cid 父目录 id；根目录传 "0"
     */
    suspend fun listFolder(cid: String): Result<List<ParsedFileItem>> {
        return runCatching {
            val resp = api.listFiles(cid = cid, showDir = 1, fcMix = 1)
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
            val body = resp.body() ?: throw IllegalStateException("empty body")
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1} cid=$cid"
                throw IllegalStateException(msg)
            }
            body.data
        }.map { items ->
            items.mapNotNull { parseRaw(it) }.distinctBy { it.id }
        }
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