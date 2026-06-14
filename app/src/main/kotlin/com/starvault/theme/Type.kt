@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.starvault.R

val Inter: FontFamily = FontFamily(
    Font(
        resId        = R.font.inter_variable,
        weight       = FontWeight.Medium,
    ),
)

@Immutable
data class StarVaultTypography(
    val display:  TextStyle = TextStyle(fontFamily = Inter, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    val title:    TextStyle = TextStyle(fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    val large:    TextStyle = TextStyle(fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    val subtitle: TextStyle = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    val body:     TextStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp),
    val caption:  TextStyle = TextStyle(fontFamily = Inter, fontSize = 12.sp),
    val micro:    TextStyle = TextStyle(fontFamily = Inter, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    val mono:     TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
)
