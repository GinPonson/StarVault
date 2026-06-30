package com.starvault.data.repository

import com.starvault.core.ServiceRateLimiter
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.FileListResponse
import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.OpenFolderAddData
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.remote.cloud115.requireSuccessful
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
    private val api: OpenFileApiService,
    private val rateLimiter: ServiceRateLimiter,
) {
    /**
     * 列出某目录的子项（单页）。
     *
     * @param cid   父目录 id；根目录传 "0"
     * @param offset  分页偏移（首屏 = 0）
     * @param limit   单页大小（默认 [DEFAULT_PAGE_SIZE] = 50）
     * @param order   排序字段（115 webapi `o` 参数）；默认 `user_ptime`（修改时间降序）
     *                常用值：
     *                - `user_ptime`  修改时间
     *                - `user_utime`  创建时间
     *                - `user_intime` 上传时间
     *                - `file_size`   文件大小
     *                - `file_name`   文件名
     *                - `file_type`   文件类型
     * @param asc     升降序（115 webapi `asc` 参数）；0 = 降序，1 = 升序
     */
    suspend fun listFolder(
        cid: String,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE,
        order: String = DEFAULT_ORDER,
        asc: Int = DEFAULT_ASC,
    ): Result<PagedFiles> = rateLimiter.acquire {
        runCatching {
            val body = api.listFiles(
                cid = cid, offset = offset, limit = limit,
                showDir = 1, fcMix = 1,
                order = order, asc = asc,
            ).requireSuccessful()
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

    /**
     * 搜索文件 / 文件夹（全账号，按文件名匹配，115 后端 substring）。
     *
     *  调用 115 `GET /files/search`（参考 p115client/client.py:15425）。
     *  响应 shape 与 listFiles 相同（顶层 state/count/data/path/order），复用 [parseRaw]。
     *
     * @param searchValue 搜索关键词（**必填**，空字符串 115 返回空）
     * @param offset      分页偏移（首屏 = 0）
     * @param limit       单页大小
     * @param order       排序字段（同 [listFolder.order]）
     * @param asc         升降序（同 [listFolder.asc]）
     */
    suspend fun searchFiles(
        searchValue: String,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE,
        order: String = DEFAULT_ORDER,
        asc: Int = DEFAULT_ASC,
    ): Result<PagedFiles> = rateLimiter.acquire {
        runCatching {
            val body = api.searchFiles(
                searchValue = searchValue,
                offset = offset,
                limit = limit,
                order = order,
                asc = asc,
                showDir = 1,
                fcMix = 1,
            ).requireSuccessful()
            if (!body.isOk) {
                val msg = body.error ?: "errno=${body.errNo ?: body.errno ?: -1}"
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

    /**
     * 在指定父目录下新建空文件夹。
     *
     *  调用 115 `POST /open/folder/add`(参考 OpenListTeam/115-sdk-go/fs.go:17-25 Mkdir)。
     *  成功后 ViewModel 通常会调 [listFolder] 刷新当前目录列表(115 端不会通过 push 推过来)。
     *
     * @param name  新文件夹名(已由 UI 层 trim,不为空)
     * @param pid   父目录 cid;根目录传 "0"
     * @return      新建文件夹的 [OpenFolderAddData](含 file_id + 回显 file_name)
     *
     *  错误处理：
     *  - HTTP / 业务失败 → Result.failure,message 来自 115 error/message 字段
     *  - 重名 / 路径非法 → 115 端 state=false + message 非空,直接抛 IllegalStateException
     */
    suspend fun createFolder(name: String, pid: String): Result<OpenFolderAddData> = rateLimiter.acquire {
        runCatching {
            val body = api.addFolder(pid = pid, fileName = name).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "新建文件夹失败")
            }
            body.data ?: throw IllegalStateException("响应为空")
        }
    }

    /**
     * 批量删除文件 / 文件夹(走 115 回收站,7 天内可恢复;客户端 UI 不提供恢复)。
     *
     *  调用 115 `POST /open/ufile/delete`,`file_ids` 字段是**逗号分隔字符串**(对齐
     *  OpenList `strings.Join(ids, ",")`;Retrofit @Field 不能接 List<String>)。
     *
     * @param ids  要删除的 fid 列表;为空时直接 Result.success 不发请求
     * @return     成功 Result.success(Unit);失败 Result.failure(IllegalStateException,message
     *             来自 115 state=false.message 或 errno=code)
     *
     *  错误处理:对齐 [createFolder] 模式 — HTTP 失败 / state=false → IllegalStateException
     */
    suspend fun deleteFiles(ids: List<String>): Result<Unit> {
        if (ids.isEmpty()) return Result.success(Unit)
        return rateLimiter.acquire {
            runCatching {
                val body = api.deleteFiles(fileIds = ids.joinToString(",")).requireSuccessful()
                if (body.state != true) {
                    throw IllegalStateException(body.message ?: "删除失败")
                }
            }
        }
    }

    /**
     * 批量移动文件 / 文件夹到目标目录。
     *
     *  调用 115 `POST /open/ufile/move`,`file_ids` 同 [deleteFiles] 逗号分隔。
     *  目标目录需为有效 folder cid(根目录传 "0");UI 层 (FolderPicker) 负责排除当前已选
     *  及其祖先 cid 防自循环,此方法不做校验。
     *
     * @param ids   要移动的 fid 列表;为空时直接 Result.success 不发请求
     * @param toCid 目标目录 cid
     * @return      成功 Result.success(Unit);失败 Result.failure(IllegalStateException,message
     *              来自 115 state=false.message 或 errno=code)
     *
     *  错误处理:对齐 [createFolder] 模式
     */
    suspend fun moveFiles(ids: List<String>, toCid: String): Result<Unit> {
        if (ids.isEmpty()) return Result.success(Unit)
        return rateLimiter.acquire {
            runCatching {
                val body = api.moveFiles(fileIds = ids.joinToString(","), toCid = toCid).requireSuccessful()
                if (body.state != true) {
                    throw IllegalStateException(body.message ?: "移动失败")
                }
            }
        }
    }

    /**
     * 重命名单个文件 / 文件夹(115 update 端点一次只能改一个)。
     *
     *  调用 115 `POST /open/ufile/update` 只传 `file_id` + `file_name`,**不传 `star`**
     *  字段(Retrofit 2.6+ 的 @Field null 自动从 form body 省略,不会误清星标 — 见
     *  [OpenFileApiService.updateFile] 注释)。
     *
     *  批量重命名:115 端点单文件限制,UI 层 [FilesViewModel] 走 N>1 直接 ToastBus.error
     *  拒绝,只在 N==1 时弹 RenameDialog 调此方法;不在此方法内做循环(避免 partial fail
     *  难回滚)。
     *
     * @param id      要重命名的 fid
     * @param newName 新文件名(已由 UI 层 trim 完;为空不抛 — 改由 VM 拦截)
     * @return        成功 Result.success(Unit);失败 Result.failure(IllegalStateException,
     *                message 来自 115 state=false.message)
     *
     *  错误处理:对齐 [createFolder] 模式
     */
    suspend fun renameFile(id: String, newName: String): Result<Unit> = rateLimiter.acquire {
        runCatching {
            val body = api.updateFile(fileId = id, fileName = newName).requireSuccessful()
            if (body.state != true) {
                throw IllegalStateException(body.message ?: "重命名失败")
            }
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

        /**
         * 默认排序字段 = `user_ptime`（修改时间）。
         * 对齐 115 webapi /files 的默认行为，避免显式传参导致与"按修改时间"UI 不一致。
         */
        const val DEFAULT_ORDER = "user_ptime"

        /**
         * 默认排序方向 = 0（降序）。修改时间最新的排在最上面。
         */
        const val DEFAULT_ASC = 0
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
        // 兼容 proapi(fn/pid/fs/upt/thumb) + webapi(n/cid/s/tp/u)两种返回形态
        val name = obj.stringOrNull("fn") ?: obj.stringOrNull("n") ?: return null
        val fid = obj.stringOrNull("fid") ?: return null
        val parentId = obj.stringOrNull("pid") ?: obj.stringOrNull("cid") ?: "0"
        val pc = obj.stringOrNull("pc") ?: ""

        // 判别 file/folder:文件有 ico / sha1 / fs,文件夹都没有
        val isFile = obj.stringOrNull("ico") != null ||
                     obj.stringOrNull("sha1") != null ||
                     obj["fs"] != null

        return if (isFile) {
            val sizeBytes = obj.longOrZero("fs") ?: obj.longOrZero("s") ?: 0L
            val ico = obj.stringOrNull("ico") ?: ""
            val fc = obj.intOrZero("fc")
            val playLong = obj.intOrZero("play_long")
            val sha1 = obj.stringOrNull("sha1") ?: obj.stringOrNull("sha") ?: ""
            val mtime = obj.longOrZero("upt") ?: obj.longOrZero("tp") ?: 0L
            // 缩略图 URL:proapi `thumb`,webapi `u`(字段名跟 OpenList `drivers/115_open/meta.go:Thumb()`
            // 完全一致,1:1 复制 `o.Thumbnail` — 直拿 115 默认 `_100`(100px / 17KB),不做 size 重写)。
            //
            // 已知 115 CDN 端硬限制:`8-bit palette 256 色量化` 任何 size 档位都强制 banding,
            // 跟 `_0`(370px / 40KB)还是 `_100` 一样糊,所以不浪费一次正则替换。
            //
            // `uo` 字段(p115client 标 "source_url")实测跟 `thumb` 同 MD5,不进 fallback 链。
            //
            // Coil 下游用 size hint downsample 到目标像素(`FilesScreen` 列表行 100dp @ 3x ≈ 300px
            // 物理像素,`_100` downscale 后可能略糊但跟 OpenList 行为 1:1)。
            val thumbnailUrl = obj.stringOrNull("thumb") ?: obj.stringOrNull("u") ?: ""
            ParsedFileItem(
                id = fid,
                parentId = parentId,
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
            val mtime = obj.longOrZero("upt") ?: obj.longOrZero("tp") ?: 0L
            // 文件夹 id:webapi 用 cid;proapi 也回 fid(且 fid != pid)
            val folderId = fid
            ParsedFileItem(
                id = folderId,
                parentId = parentId,
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