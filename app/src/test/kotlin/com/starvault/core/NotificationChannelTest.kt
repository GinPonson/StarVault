package com.starvault.core

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UploadNotificationChannel 单元测试 — Robolectric 跑在 JVM,模拟 API 26+ NotificationManager。
 *
 * 验证:
 *  - ensureCreated(ctx) 调用后,NotificationManager.getNotificationChannel 返回非 null
 *  - channel name + importance 设置正确
 *  - 多次调用 idempotent(Android 系统本身就是 idempotent,但我们包一层防止 init log 噪音)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])  // Robolectric 4.13 最高支持 API 34;>= 26 走 NotificationChannel 路径
class UploadNotificationChannelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun `ensureCreated registers upload channel with name 文件上传`() {
        UploadNotificationChannel.ensureCreated(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(UploadNotificationChannel.CHANNEL_ID)
        assertNotNull("channel should be registered", channel)
        assertEquals(UploadNotificationChannel.DISPLAY_NAME, channel.name.toString())
    }

    @Test fun `ensureCreated sets importance to LOW (no sound, no vibration)`() {
        UploadNotificationChannel.ensureCreated(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(UploadNotificationChannel.CHANNEL_ID)!!
        // 上传 progress 通知 — 用户不需要被声音打扰,选 LOW
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    }

    @Test fun `ensureCreated is idempotent`() {
        // 多次调用不抛(Robolectric 上 Android 系统本身就允许重复注册)
        UploadNotificationChannel.ensureCreated(context)
        UploadNotificationChannel.ensureCreated(context)
        UploadNotificationChannel.ensureCreated(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(UploadNotificationChannel.CHANNEL_ID)
        assertNotNull(channel)
    }
}
