package com.starvault.screenshot

import app.cash.paparazzi.DeviceConfig

/**
 * 全局 Paparazzi 截图设备配置 — Pixel 5 等比缩到 412×900，模拟中型 Android 机。
 *
 * 与 design 目录 html 的 412×900 viewport 一致，确保 baseline 截图能 1:1 对照设计稿。
 */
val PHONE_412_900: DeviceConfig = DeviceConfig.PIXEL_5.copy(
    screenWidth = 412,
    screenHeight = 900,
    xdpi = 440,
    ydpi = 440,
    softButtons = false,
)