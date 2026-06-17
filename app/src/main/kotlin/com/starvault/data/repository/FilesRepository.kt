package com.starvault.data.repository

import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.FileApiService
import com.starvault.data.remote.cloud115.ParsedFileItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Files 列表仓库：聚合 115 webapi /files 端点，转成 [ParsedFileItem]。
 *
 *  调用模式：
 *  - listFolder(cid) → 同一 cid 并行 2 次请求（show_dir=1 仅文件夹 + show_dir=0 仅文件），
 *    合并为按"文件夹优先"顺序的统一列表
 *
 *  错误处理（partial-success，与 AuthRepository.fetchUserInfo 一致）：
 *  - 两端点都成功 → 合并返回
 *  - 仅一端成功 → 返回成功的部分（让 UI 仍能展示）
 *  - 两端都失败 → Result.failure
 *
 *  ⚠️ 115 /files 的怪点：同一个端点 show_dir=1 / 0 返回**完全不同的字段**，
 *  文件夹没有 fid 字段，文件有 fid + s(字节) + ico(扩展名)。所以**必须**并行 2 次，
 *  不能用单次请求"过滤出文件夹/文件"。
 */
class FilesRepository(
    private val api: FileApiService,
) {
    /**
     * 列出某目录的所有直接子项（文件夹 + 文件）。
     *
     * @param cid 父目录 id；根目录传 "0"
     */
    suspend fun listFolder(cid: String): Result<List<ParsedFileItem>> = coroutineScope {
        val foldersDeferred = async { runCatching { fetchRaw(cid, showDir = 1) } }
        val filesDeferred   = async { runCatching { fetchRaw(cid, showDir = 0) } }
        val foldersResult = foldersDeferred.await()
        val filesResult   = filesDeferred.await()

        when {
            foldersResult.isSuccess && filesResult.isSuccess -> {
                val folders = foldersResult.getOrThrow().mapNotNull(::parseRaw)
                val files   = filesResult.getOrThrow().mapNotNull(::parseRaw)
                Result.success(folders + files)
            }
            foldersResult.isSuccess ->
                Result.success(foldersResult.getOrThrow().mapNotNull(::parseRaw))
            filesResult.isSuccess ->
                Result.success(filesResult.getOrThrow().mapNotNull(::parseRaw))
            else -> Result.failure(
                foldersResult.exceptionOrNull() ?: filesResult.exceptionOrNull()
                    ?: IllegalStateException("listFolder failed")
            )
        }
    }

    /**
     * 调一次 /files，返回原始 JsonElement 列表（保留 file/folder 两种 shape）。
     * 失败时抛 IllegalStateException，由调用方 runCatching 包。
     */
    private suspend fun fetchRaw(cid: String, showDir: Int): List<JsonElement> {
        val resp = api.listFiles(cid = cid, showDir = showDir)
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
        val body = resp.body() ?: throw IllegalStateException("empty body")
        if (!body.isOk) {
            val msg = body.error ?: "errno=${body.errNo ?: body.errNo} showDir=$showDir"
            throw IllegalStateException(msg)
        }
        return body.data
    }

    /**
     * 把单个 JsonElement（folder 或 file shape）解析成 [ParsedFileItem]。
     *
     *  判别：folder → 无 `fid` 字段，文件夹 id 用 `cid`
     *         file   → 有 `fid`，文件 id 用 `fid`，父目录用 `cid`
     *
     *  解析失败返回 null（被 mapNotNull 过滤，不影响其他 item）。
     */
    private fun parseRaw(element: JsonElement): ParsedFileItem? {
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return null
        val name = obj.stringOrNull("n") ?: return null
        val parentCid = obj.stringOrNull("cid") ?: "0"
        val pc = obj.stringOrNull("pc") ?: ""

        return if ("fid" in obj) {
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