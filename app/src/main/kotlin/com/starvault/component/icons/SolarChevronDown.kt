/*
 * Solar Bold "Alt Arrow Down" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows/Alt%20Arrow%20Down.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarChevronDown: ImageVector
    get() {
        val current = _chevronDown
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarChevronDown",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12.3704 15.8351 L18.8001 9.20467 C19.2013 8.79094 18.9581 8 18.4297 8 H5.5703 C5.04189 8 4.79869 8.79094 5.1999 9.20467 L11.6296 15.8351 C11.8427 16.055 12.1573 16.0549 12.3704 15.8351Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12.3704 15.8351
                moveTo(x = 12.3704f, y = 15.8351f)
                // L 18.8001 9.20467
                lineTo(x = 18.8001f, y = 9.20467f)
                // C 19.2013 8.79094 18.9581 8 18.4297 8
                curveTo(
                    x1 = 19.2013f,
                    y1 = 8.79094f,
                    x2 = 18.9581f,
                    y2 = 8.0f,
                    x3 = 18.4297f,
                    y3 = 8.0f,
                )
                // H 5.5703
                horizontalLineTo(x = 5.5703f)
                // C 5.04189 8 4.79869 8.79094 5.1999 9.20467
                curveTo(
                    x1 = 5.04189f,
                    y1 = 8.0f,
                    x2 = 4.79869f,
                    y2 = 8.79094f,
                    x3 = 5.1999f,
                    y3 = 9.20467f,
                )
                // L 11.6296 15.8351
                lineTo(x = 11.6296f, y = 15.8351f)
                // C 11.8427 16.055 12.1573 16.0549 12.3704 15.8351z
                curveTo(
                    x1 = 11.8427f,
                    y1 = 16.055f,
                    x2 = 12.1573f,
                    y2 = 16.0549f,
                    x3 = 12.3704f,
                    y3 = 15.8351f,
                )
                close()
            }
        }.build().also { _chevronDown = it }
    }


@Suppress("ObjectPropertyName")
private var _chevronDown: ImageVector? = null
