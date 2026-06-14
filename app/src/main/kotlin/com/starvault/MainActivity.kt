package com.starvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Phase 1 入口 Activity。
 *
 * 当前的 `setContent` 是占位（T12 会装上 `StarVaultTheme { StarVaultApp() }`），
 * 暂时只验证：
 *  - `enableEdgeToEdge()` 正常（透明状态栏/导航栏）
 *  - 编译链路通：Kotlin → Compose → Material3 → AGP 9.1 → Gradle 9
 *  - 安装到设备后能看到 #FAFAFA 底色（themes.xml windowBackground）
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { /* StarVaultTheme { StarVaultApp() } — added in T12 */ }
    }
}
