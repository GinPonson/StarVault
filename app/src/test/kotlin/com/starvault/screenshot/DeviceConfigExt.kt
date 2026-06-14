package com.starvault.screenshot

import app.cash.paparazzi.DeviceConfig

/**
 * 全局 Paparazzi 截图设备配置 — 412×900 dp 中型 Android 机。
 *
 * 与 design 目录 html 的 412×900 viewport 一致（CSS pixel = dp），baseline 截图能 1:1 对照设计稿。
 *
 * 关键点：PIXEL_5 默认 xdpi=440（xxhdpi, 2.75x 密度），其原生 1080px 屏幕是 393dp。
 * 若我们想 412dp 宽，只 screenWidth=412 + xdpi=440 是不行的 — layoutlib 会按 2.75x 密度
 * 把 412px 解释为 ~150dp，导致 9 屏全部"列宽不够、Text 字符级换行"等 layout 回归。
 *
 * 解决：screenWidth = 412dp × 2.75 = 1133px（保持 PIXEL_5 的 440dpi 不变），
 * 让 layoutlib 算出 412dp 渲染宽度，与 design viewport 1:1 对齐。
 */
val PHONE_412_900: DeviceConfig = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1133,     // 412dp × (440/160) = 1133px @ 440dpi → 412dp
    screenHeight = 2475,    // 900dp × (440/160) = 2475px @ 440dpi → 900dp
    xdpi = 440,
    ydpi = 440,
    softButtons = false,
)