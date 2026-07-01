/*
 * Solar Bold "Alt Arrow Left" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows/Alt%20Arrow%20Left.svg
 *
 * Maps to StarVault Icons.Back. Solar Bold has no AutoMirrored equivalent —
 * call sites apply `Modifier.graphicsLayer(scaleX = -1f)` when LocalLayoutDirection == Rtl.
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarBack: ImageVector
    get() {
        val current = _solarBack
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarBack",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M8.16485 11.6296 L14.7953 5.1999 C15.2091 4.79869 16 5.04189 16 5.5703 L16 18.4297 C16 18.9581 15.2091 19.2013 14.7953 18.8001 L8.16485 12.3704 C7.94505 12.1573 7.94505 11.8427 8.16485 11.6296Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 8.16485 11.6296
                moveTo(x = 8.16485f, y = 11.6296f)
                // L 14.7953 5.1999
                lineTo(x = 14.7953f, y = 5.1999f)
                // C 15.2091 4.79869 16 5.04189 16 5.5703
                curveTo(
                    x1 = 15.2091f,
                    y1 = 4.79869f,
                    x2 = 16.0f,
                    y2 = 5.04189f,
                    x3 = 16.0f,
                    y3 = 5.5703f,
                )
                // L 16 18.4297
                lineTo(x = 16.0f, y = 18.4297f)
                // C 16 18.9581 15.2091 19.2013 14.7953 18.8001
                curveTo(
                    x1 = 16.0f,
                    y1 = 18.9581f,
                    x2 = 15.2091f,
                    y2 = 19.2013f,
                    x3 = 14.7953f,
                    y3 = 18.8001f,
                )
                // L 8.16485 12.3704
                lineTo(x = 8.16485f, y = 12.3704f)
                // C 7.94505 12.1573 7.94505 11.8427 8.16485 11.6296z
                curveTo(
                    x1 = 7.94505f,
                    y1 = 12.1573f,
                    x2 = 7.94505f,
                    y2 = 11.8427f,
                    x3 = 8.16485f,
                    y3 = 11.6296f,
                )
                close()
            }
        }.build().also { _solarBack = it }
    }

@Suppress("ObjectPropertyName")
private var _solarBack: ImageVector? = null
