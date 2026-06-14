package com.starvault.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
 * 入口 Composable：包 CompositionLocalProvider + MaterialTheme。
 *
 * M3 colorScheme 只透出 4 个最常用的色：primary/onPrimary/background/surface，
 * 其余用 surfaceTint=Transparent 避免 M3 在 elevation > 0 时自动叠加 tint。
 */
@Composable
fun StarVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStarVaultColors     provides StarVaultColors(),
        LocalStarVaultTypography provides StarVaultTypography(),
        LocalStarVaultShapes     provides StarVaultShapes(),
        LocalStarVaultDimens     provides StarVaultDimens(),
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary     = StarVaultColors().accent,
                onPrimary   = StarVaultColors().accentOn,
                background  = StarVaultColors().bg,
                surface     = StarVaultColors().surface,
                onSurface   = StarVaultColors().fg,
                surfaceTint = Color.Transparent,
            ),
            content = content,
        )
    }
}
