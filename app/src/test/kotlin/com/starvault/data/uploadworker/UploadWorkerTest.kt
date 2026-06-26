package com.starvault.data.uploadworker

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.testing.TestListenableWorkerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UploadWorker Robolectric 集成测试 — 验证 ForegroundInfo + Notification 内容。
 *
 * 关键约束:
 *  - API 26+: Notification 必走 channel,channel id = "upload"
 *  - API 34+: ForegroundInfo 必带 type = FOREGROUND_SERVICE_TYPE_DATA_SYNC
 *  - Notification text 必须含 "正在上传 {fileName}" + percent
 *  - setOngoing(true) — 用户不能左划清除
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UploadWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun inputData(fileName: String, sizeBytes: Long, transferred: Long = 0L): Data {
        // 用 builder 而不是 workDataOf — 显式 Pair<String, Any?> 类型让编译器满意
        val b = Data.Builder()
        b.putString(UploadWorker.Key.FileName, fileName)
        b.putLong(UploadWorker.Key.SizeBytes, sizeBytes)
        b.putString(UploadWorker.Key.TargetCid, "0")
        b.putString(UploadWorker.Key.Uri, "content://test/uri")
        b.putLong(UploadWorker.ProgressKey.Transferred, transferred)
        return b.build()
    }

    @Test fun `getForegroundInfo returns ForegroundInfo with notification`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData("test.bin", 1000L, 250L))
            .build()

        val fg = worker.getForegroundInfoAsync().get()
        assertNotNull("getForegroundInfo should return non-null ForegroundInfo", fg)
        assertEquals(UploadWorker.NOTIFICATION_ID, fg.notificationId)
        assertNotNull("notification should be non-null", fg.notification)
    }

    @Test fun `getForegroundInfo notification text contains fileName and percent`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData("vacation.mp4", 1000L, 250L))
            .build()

        val fg = worker.getForegroundInfoAsync().get()
        val extras = fg.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        assertTrue("expected fileName in title, got: $title", title.contains("vacation.mp4"))
        assertTrue("expected 25% in text, got: $text", text.contains("25%"))
    }

    @Test fun `getForegroundInfo sets ongoing flag so user cannot swipe away`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData("x.bin", 100L))
            .build()

        val fg = worker.getForegroundInfoAsync().get()
        val n = fg.notification
        assertTrue(
            "expected FLAG_ONGOING_EVENT set, got flags=0x${n.flags.toString(16)}",
            (n.flags and Notification.FLAG_ONGOING_EVENT) != 0,
        )
    }

    @Test fun `getForegroundInfo API 34 sets FOREGROUND_SERVICE_TYPE_DATA_SYNC`() = runBlocking {
        val worker = TestListenableWorkerBuilder<UploadWorker>(context, inputData("x.bin", 100L))
            .build()

        val fg = worker.getForegroundInfoAsync().get()
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            fg.foregroundServiceType,
        )
    }

    private fun <T> runBlocking(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
