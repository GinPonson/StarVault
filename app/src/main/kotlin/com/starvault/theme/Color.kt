package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class StarVaultColors(
    val bg:         Color = Color(0xFFFAFAFA),
    val surface:    Color = Color(0xFFFFFFFF),
    val fg:         Color = Color(0xFF111111),
    val muted:      Color = Color(0xFF6B6B6B),
    val border:     Color = Color(0xFFE5E5E5),
    val accent:     Color = Color(0xFF2F6FEB),
    val accentOn:   Color = Color(0xFFFFFFFF),
    val accentSoft: Color = Color(0x142F6FEB),
    val tag1:       Color = Color(0xFF2F6FEB),
    val tag2:       Color = Color(0xFF9333EA),
    val tag3:       Color = Color(0xFFEA580C),
    val tag4:       Color = Color(0xFF16A34A),
    val tag5:       Color = Color(0xFFDB2777),
    val success:    Color = Color(0xFF17A34A),
    val warn:       Color = Color(0xFFEAB308),
    val danger:     Color = Color(0xFFDC2626),
)
