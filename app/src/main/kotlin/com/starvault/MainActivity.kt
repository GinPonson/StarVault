package com.starvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.starvault.theme.StarVaultTheme

/**
 * Phase 1 入口 Activity。
 *
 *  - [enableEdgeToEdge]：开启 edge-to-edge，状态栏/导航栏与内容融为一体（与 design HTML 一致）
 *  - [StarVaultTheme]：提供 4 个 [androidx.compose.runtime.CompositionLocal]
 *    （colors / typography / shapes / dimens）并装上 [androidx.compose.material3.MaterialTheme]
 *  - [StarVaultApp]：根 Composable，内含 Scaffold + BottomNavBar + NavHost，
 *    所有 9 个 Phase-1 屏由其 NavHost 内的 `composable<Route.X>` 装载
 *
 * 这里不再做任何业务逻辑——所有 UI 入口都收敛到 [StarVaultApp]。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StarVaultTheme {
                StarVaultApp()
            }
        }
    }
}
