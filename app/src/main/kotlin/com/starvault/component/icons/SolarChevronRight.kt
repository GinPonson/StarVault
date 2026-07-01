/*
 * Solar Bold "Alt Arrow Right" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows/Alt%20Arrow%20Right.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarChevronRight: ImageVector
    get() {
        val current = _chevronRight
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarChevronRight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M15.8351 11.6296 L9.20467 5.1999 C8.79094 4.79869 8 5.04189 8 5.5703 L8 18.4297 C8 18.9581 8.79094 19.2013 9.20467 18.8001 L15.8351 12.3704 C16.055 12.1573 16.0549 11.8427 15.8351 11.6296Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 15.8351 11.6296
                moveTo(x = 15.8351f, y = 11.6296f)
                // L 9.20467 5.1999
                lineTo(x = 9.20467f, y = 5.1999f)
                // C 8.79094 4.79869 8 5.04189 8 5.5703
                curveTo(
                    x1 = 8.79094f,
                    y1 = 4.79869f,
                    x2 = 8.0f,
                    y2 = 5.04189f,
                    x3 = 8.0f,
                    y3 = 5.5703f,
                )
                // L 8 18.4297
                lineTo(x = 8.0f, y = 18.4297f)
                // C 8 18.9581 8.79094 19.2013 9.20467 18.8001
                curveTo(
                    x1 = 8.0f,
                    y1 = 18.9581f,
                    x2 = 8.79094f,
                    y2 = 19.2013f,
                    x3 = 9.20467f,
                    y3 = 18.8001f,
                )
                // L 15.8351 12.3704
                lineTo(x = 15.8351f, y = 12.3704f)
                // C 16.055 12.1573 16.0549 11.8427 15.8351 11.6296z
                curveTo(
                    x1 = 16.055f,
                    y1 = 12.1573f,
                    x2 = 16.0549f,
                    y2 = 11.8427f,
                    x3 = 15.8351f,
                    y3 = 11.6296f,
                )
                close()
            }
        }.build().also { _chevronRight = it }
    }


@Suppress("ObjectPropertyName")
private var _chevronRight: ImageVector? = null
