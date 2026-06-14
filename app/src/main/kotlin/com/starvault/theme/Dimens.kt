package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距 / 高度 token。
 *
 *  - borderHairline 1dp   mockup `--border` 实线
 *  - spaceXs/Sm/Md/Lg/Xl  4 / 8 / 12 / 16 / 24，遵循 4dp 网格
 *  - bottomBarHeight 64dp mockup `.bt-bar` 高
 *  - bottomBarBottomGap 46dp 设备区与底栏之间留白
 */
@Immutable
data class StarVaultDimens(
    val borderHairline:     Dp = 1.dp,
    val spaceXs:            Dp = 4.dp,
    val spaceSm:            Dp = 8.dp,
    val spaceMd:            Dp = 12.dp,
    val spaceLg:            Dp = 16.dp,
    val spaceXl:            Dp = 24.dp,
    val bottomBarHeight:    Dp = 64.dp,
    val bottomBarBottomGap: Dp = 46.dp,
)
