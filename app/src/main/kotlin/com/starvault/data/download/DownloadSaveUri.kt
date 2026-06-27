package com.starvault.data.download

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.OutputStream

/**
 * 115 下载落盘辅助 — `MediaStore.Downloads` 公开目录写入(API 29+,免 SAF picker)。
 *
 * ## 三步生命周期
 *  1. [prepare]            : insert(IS_PENDING=1) → Uri(其他 app 看不到这条)
 *  2. [openOutputStream]   : ContentResolver.openOutputStream(uri, "w") → 流
 *  3a. [publish]           : 写完后 update(IS_PENDING=0) → 文件对其他 app 可见
 *  3b. [delete]            : 写失败 / 取消时 delete(uri) → 清 IS_PENDING=1 残留
 *
 * ## 为什么走 IS_PENDING 两阶段
 *  - 直接 insert(IS_PENDING=0) 后立即写流,写期间 app 被杀 → 0 字节 / 截断的文件暴露在
 *    系统 Downloads 目录里(用户能看到,删起来还麻烦)。
 *  - IS_PENDING=1 时该行对其他 app 不可见;完成后 flip 到 0 才"上线",中途失败直接
 *    delete 不留痕迹(spec §1.4 + plan 风险 #7)。
 *
 * ## MIME 推断
 *  - 从 [fileName] 扩展名查 [MimeTypeMap];无扩展名 / 查不到 → `application/octet-stream`
 *    兜底(spec plan "MIME fallback")。
 *
 * ## 重名
 *  - 不主动加 `_${timestamp}` 后缀 — Android MediaStore.Downloads 允许重名,系统会自动
 *    命名为 `name (1).mp4` / `name (2).mp4` 等,与 spec §12 #2 表述略有差异但更干净。
 *
 * ## 测试
 *  - 构造时注入 [ContentResolver](不是 Context),便于 Robolectric 直接 mock。
 */
class DownloadSaveUri(
    private val contentResolver: ContentResolver,
) {

    /**
     * 在 `Downloads/` 下新建一条 IS_PENDING=1 的 MediaStore 行。
     *
     * @return 成功 → 新行的 [Uri];`null` 表示 insert 失败(罕见,通常 OOM 或磁盘满)。
     */
    fun prepare(fileName: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // IS_PENDING=1 → 系统其他 app 看不到这条;update(0) 后才"上线"
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }

    /**
     * 打开写流 — 调用方拿到后写入文件内容。
     *
     * 返回的流**未 buffer**,由 [com.starvault.data.download.OssDownloader] 内部
     * `output.buffered()` 包一层。
     */
    fun openOutputStream(uri: Uri): OutputStream? =
        contentResolver.openOutputStream(uri, "w")

    /**
     * 写流完成,翻 IS_PENDING=0 — 文件正式出现在系统 Downloads 目录里,其他 app 可访问。
     */
    fun publish(uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        try {
            contentResolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            Log.w(TAG, "publish (IS_PENDING=0) failed for $uri", t)
        }
    }

    /**
     * 写流失败 / 取消 — 清掉 IS_PENDING=1 的 ghost 行,避免 Downloads 目录留 0 字节文件。
     * 静默忽略失败(delete 找不到行 / 权限问题),用户看不到副作用。
     */
    fun delete(uri: Uri) {
        try {
            contentResolver.delete(uri, null, null)
        } catch (t: Throwable) {
            Log.w(TAG, "delete ghost row failed for $uri", t)
        }
    }

    companion object {

        private const val TAG = "DownloadSaveUri"

        private const val DEFAULT_MIME = "application/octet-stream"

        /**
         * 从文件名推断 MIME — 找不到扩展名或扩展名无 MIME 映射时 fallback 到
         * `application/octet-stream`(spec plan "MIME fallback")。
         */
        fun mimeTypeFromName(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext.isBlank()) return DEFAULT_MIME
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: DEFAULT_MIME
        }
    }
}
