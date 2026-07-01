/*
 * Solar Bold "Check Circle" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Check%20Circle.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarCheckBold: ImageVector
    get() {
        val current = _checkBold
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarCheckBold",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M22 12 C22 17.5228 17.5228 22 12 22 C6.47715 22 2 17.5228 2 12 C2 6.47715 6.47715 2 12 2 C17.5228 2 22 6.47715 22 12Z M16.0303 8.96967 C16.3232 9.26256 16.3232 9.73744 16.0303 10.0303 L11.0303 15.0303 C10.7374 15.3232 10.2626 15.3232 9.96967 15.0303 L7.96967 13.0303 C7.67678 12.7374 7.67678 12.2626 7.96967 11.9697 C8.26256 11.6768 8.73744 11.6768 9.03033 11.9697 L10.5 13.4393 L12.7348 11.2045 L14.9697 8.96967 C15.2626 8.67678 15.7374 8.67678 16.0303 8.96967Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
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
                // M 16.0303 8.96967
                moveTo(x = 16.0303f, y = 8.96967f)
                // C 16.3232 9.26256 16.3232 9.73744 16.0303 10.0303
                curveTo(
                    x1 = 16.3232f,
                    y1 = 9.26256f,
                    x2 = 16.3232f,
                    y2 = 9.73744f,
                    x3 = 16.0303f,
                    y3 = 10.0303f,
                )
                // L 11.0303 15.0303
                lineTo(x = 11.0303f, y = 15.0303f)
                // C 10.7374 15.3232 10.2626 15.3232 9.96967 15.0303
                curveTo(
                    x1 = 10.7374f,
                    y1 = 15.3232f,
                    x2 = 10.2626f,
                    y2 = 15.3232f,
                    x3 = 9.96967f,
                    y3 = 15.0303f,
                )
                // L 7.96967 13.0303
                lineTo(x = 7.96967f, y = 13.0303f)
                // C 7.67678 12.7374 7.67678 12.2626 7.96967 11.9697
                curveTo(
                    x1 = 7.67678f,
                    y1 = 12.7374f,
                    x2 = 7.67678f,
                    y2 = 12.2626f,
                    x3 = 7.96967f,
                    y3 = 11.9697f,
                )
                // C 8.26256 11.6768 8.73744 11.6768 9.03033 11.9697
                curveTo(
                    x1 = 8.26256f,
                    y1 = 11.6768f,
                    x2 = 8.73744f,
                    y2 = 11.6768f,
                    x3 = 9.03033f,
                    y3 = 11.9697f,
                )
                // L 10.5 13.4393
                lineTo(x = 10.5f, y = 13.4393f)
                // L 12.7348 11.2045
                lineTo(x = 12.7348f, y = 11.2045f)
                // L 14.9697 8.96967
                lineTo(x = 14.9697f, y = 8.96967f)
                // C 15.2626 8.67678 15.7374 8.67678 16.0303 8.96967z
                curveTo(
                    x1 = 15.2626f,
                    y1 = 8.67678f,
                    x2 = 15.7374f,
                    y2 = 8.67678f,
                    x3 = 16.0303f,
                    y3 = 8.96967f,
                )
                close()
            }
        }.build().also { _checkBold = it }
    }


@Suppress("ObjectPropertyName")
private var _checkBold: ImageVector? = null
