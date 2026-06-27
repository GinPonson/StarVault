package com.starvault.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 115 下载进度通知渠道 — M3 引入,镜像 [UploadNotificationChannel](同 IMPORTANCE_LOW)。
 *
 * ## 设计意图
 *  - 下载进度通知:**不需要声音/震动**(LOW importance)— 用户切到后台后,持续看到 percent
 *    即可,不应该被声音打断
 *  - Channel id 固定为 `download`(Android 一旦注册,id 不能改 — 改了就丢失用户已有的渠道设置)
 *  - 显式调用 [ensureCreated]:ServiceLocator.init 阶段 / DownloadWorker 第一次跑前 各 1 次
 *
 * ## Idempotency
 *  Android 系统允许重复注册同名 channel(返回原 channel);本类方法本身就是 no-op
 *  当 channel 已存在时,但保留显式调用是为了在 Logcat 留 trace,方便排查"渠道为啥没建"。
 */
object DownloadNotificationChannel {

    /** Channel id — Android 一旦注册不能改,改了用户已有渠道设置会丢。 */
    const val CHANNEL_ID = "download"

    /** 渠道显示名(用户能在系统设置里看到)。 */
    const val DISPLAY_NAME = "文件下载"

    /**
     * 注册 channel(API 26+)。< 26 是 no-op(那时代没 NotificationChannel)。
     *
     * 调用方:
     *  - [ServiceLocator.init](M3 引入) — 应用启动时注册
     *  - [com.starvault.data.downloadworker.DownloadWorker.getForegroundInfo] 调
     *    `setForeground` 前再注册 1 次(双保险,系统 Process 死亡后 Application 重建
     *    不一定走 onCreate)
     */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 系统方法 getNotificationChannel(id) != null 时,创建是 idempotent
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            DISPLAY_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "115 云盘下载进度"
            setShowBadge(false)  // 进度通知不显示 badge
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }
}
