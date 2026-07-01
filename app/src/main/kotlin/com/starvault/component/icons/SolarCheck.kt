/*
 * Solar Bold "Check Square" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Check%20Square.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarCheck: ImageVector
    get() {
        val current = _solarCheck
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarCheck",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 22 C7.28595 22 4.92893 22 3.46447 20.5355 C2 19.0711 2 16.714 2 12 C2 7.28595 2 4.92893 3.46447 3.46447 C4.92893 2 7.28595 2 12 2 C16.714 2 19.0711 2 20.5355 3.46447 C22 4.92893 22 7.28595 22 12 C22 16.714 22 19.0711 20.5355 20.5355 C19.0711 22 16.714 22 12 22Z M16.0303 8.96967 C16.3232 9.26256 16.3232 9.73744 16.0303 10.0303 L11.0303 15.0303 C10.7374 15.3232 10.2626 15.3232 9.96967 15.0303 L7.96967 13.0303 C7.67678 12.7374 7.67678 12.2626 7.96967 11.9697 C8.26256 11.6768 8.73744 11.6768 9.03033 11.9697 L10.5 13.4393 L14.9697 8.96967 C15.2626 8.67678 15.7374 8.67678 16.0303 8.96967Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 12 22
                moveTo(x = 12.0f, y = 22.0f)
                // C 7.28595 22 4.92893 22 3.46447 20.5355
                curveTo(
                    x1 = 7.28595f,
                    y1 = 22.0f,
                    x2 = 4.92893f,
                    y2 = 22.0f,
                    x3 = 3.46447f,
                    y3 = 20.5355f,
                )
                // C 2 19.0711 2 16.714 2 12
                curveTo(
                    x1 = 2.0f,
                    y1 = 19.0711f,
                    x2 = 2.0f,
                    y2 = 16.714f,
                    x3 = 2.0f,
                    y3 = 12.0f,
                )
                // C 2 7.28595 2 4.92893 3.46447 3.46447
                curveTo(
                    x1 = 2.0f,
                    y1 = 7.28595f,
                    x2 = 2.0f,
                    y2 = 4.92893f,
                    x3 = 3.46447f,
                    y3 = 3.46447f,
                )
                // C 4.92893 2 7.28595 2 12 2
                curveTo(
                    x1 = 4.92893f,
                    y1 = 2.0f,
                    x2 = 7.28595f,
                    y2 = 2.0f,
                    x3 = 12.0f,
                    y3 = 2.0f,
                )
                // C 16.714 2 19.0711 2 20.5355 3.46447
                curveTo(
                    x1 = 16.714f,
                    y1 = 2.0f,
                    x2 = 19.0711f,
                    y2 = 2.0f,
                    x3 = 20.5355f,
                    y3 = 3.46447f,
                )
                // C 22 4.92893 22 7.28595 22 12
                curveTo(
                    x1 = 22.0f,
                    y1 = 4.92893f,
                    x2 = 22.0f,
                    y2 = 7.28595f,
                    x3 = 22.0f,
                    y3 = 12.0f,
                )
                // C 22 16.714 22 19.0711 20.5355 20.5355
                curveTo(
                    x1 = 22.0f,
                    y1 = 16.714f,
                    x2 = 22.0f,
                    y2 = 19.0711f,
                    x3 = 20.5355f,
                    y3 = 20.5355f,
                )
                // C 19.0711 22 16.714 22 12 22z
                curveTo(
                    x1 = 19.0711f,
                    y1 = 22.0f,
                    x2 = 16.714f,
                    y2 = 22.0f,
                    x3 = 12.0f,
                    y3 = 22.0f,
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
        }.build().also { _solarCheck = it }
    }


@Suppress("ObjectPropertyName")
private var _solarCheck: ImageVector? = null
