package com.starvault.data.download

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/**
 * DownloadSaveUri Robolectric 测试 — mock ContentResolver,验证:
 *  - prepare() 写入 IS_PENDING=1 + DISPLAY_NAME + MIME + RELATIVE_PATH=Downloads
 *  - openOutputStream() 透传到 ContentResolver.openOutputStream(uri, "w")
 *  - publish() 翻 IS_PENDING=0
 *  - delete() 调 ContentResolver.delete
 *  - mimeTypeFromName() 扩展名 → MIME 映射,unknown → application/octet-stream
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadSaveUriTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val resolver = mockk<ContentResolver>(relaxed = true)
    private val saveUri = DownloadSaveUri(contentResolver = resolver)

    private val fakeDownloadsUri: Uri = Uri.parse("content://media/external/downloads")

    @Before fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
    }

    @Test fun `prepare inserts row with IS_PENDING=1, fileName, mime, Downloads path`() {
        val insertedUri = Uri.parse("content://media/external/downloads/12345")
        val valuesSlot = slot<ContentValues>()
        every { resolver.insert(eq(MediaStore.Downloads.EXTERNAL_CONTENT_URI), capture(valuesSlot)) } returns insertedUri

        val result = saveUri.prepare(fileName = "song.flac", mimeType = "audio/flac")

        assertEquals(insertedUri, result)
        val captured = valuesSlot.captured
        assertEquals("song.flac", captured.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
        assertEquals("audio/flac", captured.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        assertEquals(Environment.DIRECTORY_DOWNLOADS, captured.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals(1, captured.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
    }

    @Test fun `prepare returns null when ContentResolver insert returns null`() {
        every { resolver.insert(any(), any()) } returns null

        val result = saveUri.prepare(fileName = "x.bin", mimeType = "application/octet-stream")

        assertNull(result)
    }

    @Test fun `openOutputStream delegates to ContentResolver openOutputStream with mode w`() {
        val uri = Uri.parse("content://media/external/downloads/99")
        val expectedStream = ByteArrayOutputStream()
        every { resolver.openOutputStream(uri, "w") } returns expectedStream

        val stream = saveUri.openOutputStream(uri)

        assertNotNull(stream)
        assertEquals(expectedStream, stream)
        verify(exactly = 1) { resolver.openOutputStream(uri, "w") }
    }

    @Test fun `publish updates IS_PENDING=0`() {
        val uri = Uri.parse("content://media/external/downloads/1")
        val valuesSlot = slot<ContentValues>()
        every { resolver.update(eq(uri), capture(valuesSlot), any(), any()) } returns 1

        saveUri.publish(uri)

        val captured = valuesSlot.captured
        assertEquals(0, captured.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
    }

    @Test fun `delete calls ContentResolver delete with null selection`() {
        val uri = Uri.parse("content://media/external/downloads/1")
        every { resolver.delete(eq(uri), any(), any()) } returns 1

        saveUri.delete(uri)

        verify(exactly = 1) { resolver.delete(uri, null, null) }
    }

    @Test fun `delete swallows exceptions to keep caller clean`() {
        // 即使 ContentResolver 抛(权限 / row already gone),delete 不应向上抛
        val uri = Uri.parse("content://media/external/downloads/missing")
        every { resolver.delete(any(), any(), any()) } throws SecurityException("row gone")

        // 不应抛异常
        saveUri.delete(uri)

        verify(exactly = 1) { resolver.delete(uri, null, null) }
    }

    // ---------- mimeTypeFromName 纯函数,无 Robolectric 依赖 ----------

    @Test fun `mimeTypeFromName returns MimeTypeMap lookup result for known extension`() {
        // Robolectric 的 MimeTypeMap 表几乎为空(连 html 都没),所以 mock getSingleton
        // 验证我们的代码确实走 MimeTypeMap.getMimeTypeFromExtension 这条路,不是只走 fallback
        val fakeMimeMap = mockk<android.webkit.MimeTypeMap>(relaxed = true)
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns fakeMimeMap
        every { fakeMimeMap.getMimeTypeFromExtension("html") } returns "text/html"

        assertEquals("text/html", DownloadSaveUri.mimeTypeFromName("page.html"))
        verify(exactly = 1) { fakeMimeMap.getMimeTypeFromExtension("html") }
    }

    @Test fun `mimeTypeFromName returns octet-stream when MimeTypeMap returns null`() {
        // Robolectric 默认 null → fallback 触发
        val fakeMimeMap = mockk<android.webkit.MimeTypeMap>(relaxed = true)
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns fakeMimeMap
        every { fakeMimeMap.getMimeTypeFromExtension("flac") } returns null

        assertEquals("application/octet-stream", DownloadSaveUri.mimeTypeFromName("song.flac"))
    }

    @Test fun `mimeTypeFromName lowercases extension before MimeTypeMap lookup`() {
        // 大写扩展名 → lowercase 后再查 → 验证调的是 lowercase 后的 ext
        val fakeMimeMap = mockk<android.webkit.MimeTypeMap>(relaxed = true)
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns fakeMimeMap
        every { fakeMimeMap.getMimeTypeFromExtension("mp3") } returns "audio/mpeg"

        // 输入 .MP3(大写)→ 期望:lookup "mp3"(小写)
        assertEquals("audio/mpeg", DownloadSaveUri.mimeTypeFromName("clip.MP3"))
        verify(exactly = 1) { fakeMimeMap.getMimeTypeFromExtension("mp3") }
        verify(exactly = 0) { fakeMimeMap.getMimeTypeFromExtension("MP3") }
    }

    @Test fun `mimeTypeFromName returns octet-stream for unknown extension`() {
        // 未注册扩展名 → MimeTypeMap 返回 null → fallback
        val fakeMimeMap = mockk<android.webkit.MimeTypeMap>(relaxed = true)
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns fakeMimeMap
        every { fakeMimeMap.getMimeTypeFromExtension(any()) } returns null

        assertEquals("application/octet-stream", DownloadSaveUri.mimeTypeFromName("blob.unknownext"))
    }

    @Test fun `mimeTypeFromName returns octet-stream for file without extension`() {
        // 无扩展名 → substringAfterLast 返回 "" → 直接 fallback,不调 MimeTypeMap
        val fakeMimeMap = mockk<android.webkit.MimeTypeMap>(relaxed = true)
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns fakeMimeMap

        assertEquals("application/octet-stream", DownloadSaveUri.mimeTypeFromName("README"))
        assertEquals("application/octet-stream", DownloadSaveUri.mimeTypeFromName(""))
        verify(exactly = 0) { fakeMimeMap.getMimeTypeFromExtension(any()) }
    }

    // ---------- helpers ----------

    private fun eq(uri: Uri): Uri = uri  // mockk 不需要 eq matcher,这里只为了 IDE 提示
}
