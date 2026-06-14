package com.starvault.component

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 把当前 Composable 的 status bar 前景色切到 dark / light。
 *  - darkIcons = true   → 状态栏图标用黑色（亮底）
 *  - darkIcons = false  → 状态栏图标用白色（暗底）
 *
 * 必须包在 SideEffect 里，让每次切屏都重新应用到 Activity.window。
 */
@Composable
fun StatusBarStyle(darkIcons: Boolean) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}
