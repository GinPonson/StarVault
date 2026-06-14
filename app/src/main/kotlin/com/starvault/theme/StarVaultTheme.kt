package com.starvault.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 4 个 CompositionLocal 持有自定义 token。Composable 通过 `StarVaultTheme.colors` 访问。
 * 用 `staticCompositionLocalOf`（不读频繁变化的 state）— token 在 App 生命周期内不变。
 */
val LocalStarVaultColors     = staticCompositionLocalOf { StarVaultColors() }
val LocalStarVaultTypography = staticCompositionLocalOf { StarVaultTypography() }
val LocalStarVaultShapes     = staticCompositionLocalOf { StarVaultShapes() }
val LocalStarVaultDimens     = staticCompositionLocalOf { StarVaultDimens() }

/**
 * 全局 token accessor。Composable 内 `StarVaultTheme.colors.fg` / `.border` / `.accent` 即可。
 * 故意不挂 M3 Typography / M3 Shapes — 我们有自己的 typography / shape，挂在 CompositionLocal 上。
 */
object StarVaultTheme {
    val colors:     StarVaultColors     @Composable get() = LocalStarVaultColors.current
    val typography: StarVaultTypography @Composable get() = LocalStarVaultTypography.current
    val shapes:     StarVaultShapes     @Composable get() = LocalStarVaultShapes.current
    val dimens:     StarVaultDimens     @Composable get() = LocalStarVaultDimens.current
}

/**
 * 入口 Composable：仅 CompositionLocalProvider。
 *
 * 注：故意不挂 M3 [androidx.compose.material3.MaterialTheme]。M3 2026.05.00 BOM 在
 * Paparazzi 2.0.0-alpha04 渲染下会引入 layout 回归（fillMaxWidth 在其内置 Surface wrapper
 * 中被错误约束为内容最小宽度）。我们用自定义 typography / shapes，不依赖 M3 的 token。
 */
@Composable
fun StarVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStarVaultColors     provides StarVaultColors(),
        LocalStarVaultTypography provides StarVaultTypography(),
        LocalStarVaultShapes     provides StarVaultShapes(),
        LocalStarVaultDimens     provides StarVaultDimens(),
    ) {
        content()
    }
}
