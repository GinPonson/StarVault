/*
 * Solar Bold "Close Square" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Close%20Square.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarClose: ImageVector
    get() {
        val current = _solarClose
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarClose",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 22 C7.28595 22 4.92893 22 3.46447 20.5355 C2 19.0711 2 16.714 2 12 C2 7.28595 2 4.92893 3.46447 3.46447 C4.92893 2 7.28595 2 12 2 C16.714 2 19.0711 2 20.5355 3.46447 C22 4.92893 22 7.28595 22 12 C22 16.714 22 19.0711 20.5355 20.5355 C19.0711 22 16.714 22 12 22Z M8.96965 8.96967 C9.26254 8.67678 9.73742 8.67678 10.0303 8.96967 L12 10.9394 L13.9696 8.96969 C14.2625 8.6768 14.7374 8.6768 15.0303 8.96969 C15.3232 9.26258 15.3232 9.73746 15.0303 10.0303 L13.0606 12 L15.0303 13.9697 C15.3232 14.2625 15.3232 14.7374 15.0303 15.0303 C14.7374 15.3232 14.2625 15.3232 13.9696 15.0303 L12 13.0607 L10.0303 15.0303 C9.73744 15.3232 9.26256 15.3232 8.96967 15.0303 C8.67678 14.7374 8.67678 14.2626 8.96967 13.9697 L10.9393 12 L8.96965 10.0303 C8.67676 9.73744 8.67676 9.26256 8.96965 8.96967Z
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
                // M 8.96965 8.96967
                moveTo(x = 8.96965f, y = 8.96967f)
                // C 9.26254 8.67678 9.73742 8.67678 10.0303 8.96967
                curveTo(
                    x1 = 9.26254f,
                    y1 = 8.67678f,
                    x2 = 9.73742f,
                    y2 = 8.67678f,
                    x3 = 10.0303f,
                    y3 = 8.96967f,
                )
                // L 12 10.9394
                lineTo(x = 12.0f, y = 10.9394f)
                // L 13.9696 8.96969
                lineTo(x = 13.9696f, y = 8.96969f)
                // C 14.2625 8.6768 14.7374 8.6768 15.0303 8.96969
                curveTo(
                    x1 = 14.2625f,
                    y1 = 8.6768f,
                    x2 = 14.7374f,
                    y2 = 8.6768f,
                    x3 = 15.0303f,
                    y3 = 8.96969f,
                )
                // C 15.3232 9.26258 15.3232 9.73746 15.0303 10.0303
                curveTo(
                    x1 = 15.3232f,
                    y1 = 9.26258f,
                    x2 = 15.3232f,
                    y2 = 9.73746f,
                    x3 = 15.0303f,
                    y3 = 10.0303f,
                )
                // L 13.0606 12
                lineTo(x = 13.0606f, y = 12.0f)
                // L 15.0303 13.9697
                lineTo(x = 15.0303f, y = 13.9697f)
                // C 15.3232 14.2625 15.3232 14.7374 15.0303 15.0303
                curveTo(
                    x1 = 15.3232f,
                    y1 = 14.2625f,
                    x2 = 15.3232f,
                    y2 = 14.7374f,
                    x3 = 15.0303f,
                    y3 = 15.0303f,
                )
                // C 14.7374 15.3232 14.2625 15.3232 13.9696 15.0303
                curveTo(
                    x1 = 14.7374f,
                    y1 = 15.3232f,
                    x2 = 14.2625f,
                    y2 = 15.3232f,
                    x3 = 13.9696f,
                    y3 = 15.0303f,
                )
                // L 12 13.0607
                lineTo(x = 12.0f, y = 13.0607f)
                // L 10.0303 15.0303
                lineTo(x = 10.0303f, y = 15.0303f)
                // C 9.73744 15.3232 9.26256 15.3232 8.96967 15.0303
                curveTo(
                    x1 = 9.73744f,
                    y1 = 15.3232f,
                    x2 = 9.26256f,
                    y2 = 15.3232f,
                    x3 = 8.96967f,
                    y3 = 15.0303f,
                )
                // C 8.67678 14.7374 8.67678 14.2626 8.96967 13.9697
                curveTo(
                    x1 = 8.67678f,
                    y1 = 14.7374f,
                    x2 = 8.67678f,
                    y2 = 14.2626f,
                    x3 = 8.96967f,
                    y3 = 13.9697f,
                )
                // L 10.9393 12
                lineTo(x = 10.9393f, y = 12.0f)
                // L 8.96965 10.0303
                lineTo(x = 8.96965f, y = 10.0303f)
                // C 8.67676 9.73744 8.67676 9.26256 8.96965 8.96967z
                curveTo(
                    x1 = 8.67676f,
                    y1 = 9.73744f,
                    x2 = 8.67676f,
                    y2 = 9.26256f,
                    x3 = 8.96965f,
                    y3 = 8.96967f,
                )
                close()
            }
        }.build().also { _solarClose = it }
    }


@Suppress("ObjectPropertyName")
private var _solarClose: ImageVector? = null
