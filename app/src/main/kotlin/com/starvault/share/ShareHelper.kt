package com.starvault.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.starvault.core.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

private const val FILE_PROVIDER_AUTHORITIES_SUFFIX = ".fileprovider"

/** 分享兜底上限(50MB)— 超大图直接 throw,防止恶意/异常 URL 撑爆 cacheDir。 */
private const val MAX_SHARE_BYTES = 50L * 1024 * 1024

/** 允许分享的图片扩展名(白名单)— 其他格式走 "仅图片可分享" 拒绝。 */
private val ALLOWED_IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "heic")

/**
 * 把 mediaUrl 远程图片下载到 cacheDir,弹 Android Sharesheet 分享本地副本。
 *
 * **只支持单张图片分享** — 用户原话"只让图片的分享,不支持多个分享"。
 * Video / Audio / PDF 不走这个路径,PreviewVideo / PreviewAudio 的 MoreMenu SHARE
 * 直接 `ToastBus.info("仅图片可分享")` 占位;Files BulkBar SHARE 保留
 * `ToastBus.info("分享功能暂不支持")` 占位(批量分享用户已说不需要)。
 *
 * 流程:
 *  1. ext 白名单检查 — 仅 jpg/jpeg/png/webp/gif/heic 通过,否则 throw IOException
 *  2. OkHttp GET mediaUrl(走 ServiceLocator.okHttpClient,UA 注入已带,CDN URL 本身已签
 *     不需 Bearer 鉴权)— 对齐 M3 download pipeline 复用同一 OkHttp 客户端
 *  3. response body → 8KB 块循环写到 cacheDir/share_${ts}.{ext},
 *     累计字节 > 50MB throw("文件过大,暂不支持 > 50MB 分享")
 *  4. FileProvider.getUriForFile 拿 content:// URI(authorities = ${packageName}.fileprovider)
 *  5. Intent.ACTION_SEND + type=image + EXTRA_STREAM + FLAG_GRANT_READ_URI_PERMISSION
 *  6. Intent.createChooser 弹系统 Sharesheet
 *  7. try/finally:tempFile.deleteOnExit() + runCatching { delete() } 兜底,
 *     任何路径(下载失败 / 文件过大 / startActivity 抛 ActivityNotFoundException)
 *     都清理 cacheDir;进程退出时 deleteOnExit 再删一次
 *
 * 错误处理:任何失败 → throw IOException,UI 层 runCatching 兜底 + ToastBus.error。
 *
 * @param context  Application/Activity context(用于 cacheDir + startActivity)
 * @param mediaUrl 115 CDN 5min 签名 URL(PreviewImage.state.mediaUrl 拿)
 * @throws java.io.IOException ext 不在白名单 / HTTP 失败 / 累计字节 > 50MB
 */
suspend fun shareImage(context: Context, mediaUrl: String) {
    val ext = mediaUrl.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .take(5)
    if (ext !in ALLOWED_IMAGE_EXTS) {
        throw java.io.IOException("仅支持图片分享 (jpg/jpeg/png/webp/gif/heic), 当前: .$ext")
    }

    val tempFile = File(context.cacheDir, "share_${System.currentTimeMillis()}.$ext")
    try {
        withContext(Dispatchers.IO) {
            val resp = ServiceLocator.okHttpClient
                .newCall(Request.Builder().url(mediaUrl).build())
                .execute()
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            resp.use { r ->
                r.body?.byteStream()?.use { input ->
                    tempFile.outputStream().use { out ->
                        val buf = ByteArray(8 * 1024)
                        var written = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            written += n
                            if (written > MAX_SHARE_BYTES) {
                                throw java.io.IOException("文件过大, 暂不支持 > 50MB 分享")
                            }
                            out.write(buf, 0, n)
                        }
                    }
                }
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_AUTHORITIES_SUFFIX,
            tempFile,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享"))
    } finally {
        // 兜底清理:任何路径(包括 ActivityNotFoundException)都删 tempFile
        tempFile.deleteOnExit()
        runCatching { tempFile.delete() }
    }
}
