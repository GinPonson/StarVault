/*
 * Solar Bold "Clock Circle" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Time/Clock%20Circle.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarClock: ImageVector
    get() {
        val current = _solarClock
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarClock",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M22 12 C22 17.5228 17.5228 22 12 22 C6.47715 22 2 17.5228 2 12 C2 6.47715 6.47715 2 12 2 C17.5228 2 22 6.47715 22 12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 22 12
                moveTo(x = 22.0f, y = 12.0f)
                // C 22 17.5228 17.5228 22 12 22
                curveTo(
                    x1 = 22.0f,
                    y1 = 17.5228f,
                    x2 = 17.5228f,
                    y2 = 22.0f,
                    x3 = 12.0f,
                    y3 = 22.0f,
                )
                // C 6.47715 22 2 17.5228 2 12
                curveTo(
                    x1 = 6.47715f,
                    y1 = 22.0f,
                    x2 = 2.0f,
                    y2 = 17.5228f,
                    x3 = 2.0f,
                    y3 = 12.0f,
                )
                // C 2 6.47715 6.47715 2 12 2
                curveTo(
                    x1 = 2.0f,
                    y1 = 6.47715f,
                    x2 = 6.47715f,
                    y2 = 2.0f,
                    x3 = 12.0f,
                    y3 = 2.0f,
                )
                // C 17.5228 2 22 6.47715 22 12z
                curveTo(
                    x1 = 17.5228f,
                    y1 = 2.0f,
                    x2 = 22.0f,
                    y2 = 6.47715f,
                    x3 = 22.0f,
                    y3 = 12.0f,
                )
                close()
            }
            // M12 7.25 C12.4142 7.25 12.75 7.58579 12.75 8 V11.6893 L15.0303 13.9697 C15.3232 14.2626 15.3232 14.7374 15.0303 15.0303 C14.7374 15.3232 14.2626 15.3232 13.9697 15.0303 L11.4697 12.5303 C11.329 12.3897 11.25 12.1989 11.25 12 V8 C11.25 7.58579 11.5858 7.25 12 7.25Z
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 12 7.25
                moveTo(x = 12.0f, y = 7.25f)
                // C 12.4142 7.25 12.75 7.58579 12.75 8
                curveTo(
                    x1 = 12.4142f,
                    y1 = 7.25f,
                    x2 = 12.75f,
                    y2 = 7.58579f,
                    x3 = 12.75f,
                    y3 = 8.0f,
                )
                // V 11.6893
                verticalLineTo(y = 11.6893f)
                // L 15.0303 13.9697
                lineTo(x = 15.0303f, y = 13.9697f)
                // C 15.3232 14.2626 15.3232 14.7374 15.0303 15.0303
                curveTo(
                    x1 = 15.3232f,
                    y1 = 14.2626f,
                    x2 = 15.3232f,
                    y2 = 14.7374f,
                    x3 = 15.0303f,
                    y3 = 15.0303f,
                )
                // C 14.7374 15.3232 14.2626 15.3232 13.9697 15.0303
                curveTo(
                    x1 = 14.7374f,
                    y1 = 15.3232f,
                    x2 = 14.2626f,
                    y2 = 15.3232f,
                    x3 = 13.9697f,
                    y3 = 15.0303f,
                )
                // L 11.4697 12.5303
                lineTo(x = 11.4697f, y = 12.5303f)
                // C 11.329 12.3897 11.25 12.1989 11.25 12
                curveTo(
                    x1 = 11.329f,
                    y1 = 12.3897f,
                    x2 = 11.25f,
                    y2 = 12.1989f,
                    x3 = 11.25f,
                    y3 = 12.0f,
                )
                // V 8
                verticalLineTo(y = 8.0f)
                // C 11.25 7.58579 11.5858 7.25 12 7.25z
                curveTo(
                    x1 = 11.25f,
                    y1 = 7.58579f,
                    x2 = 11.5858f,
                    y2 = 7.25f,
                    x3 = 12.0f,
                    y3 = 7.25f,
                )
                close()
            }
        }.build().also { _solarClock = it }
    }


@Suppress("ObjectPropertyName")
private var _solarClock: ImageVector? = null
