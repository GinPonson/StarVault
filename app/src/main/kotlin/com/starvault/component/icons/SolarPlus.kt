/*
 * Solar Bold "Add Square" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Add%20Square.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPlus: ImageVector
    get() {
        val current = _solarPlus
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPlus",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 22 C7.28595 22 4.92893 22 3.46447 20.5355 C2 19.0711 2 16.714 2 12 C2 7.28595 2 4.92893 3.46447 3.46447 C4.92893 2 7.28595 2 12 2 C16.714 2 19.0711 2 20.5355 3.46447 C22 4.92893 22 7.28595 22 12 C22 16.714 22 19.0711 20.5355 20.5355 C19.0711 22 16.714 22 12 22Z M12 8.25 C12.4142 8.25 12.75 8.58579 12.75 9 V11.25 H15 C15.4142 11.25 15.75 11.5858 15.75 12 C15.75 12.4142 15.4142 12.75 15 12.75 H12.75 L12.75 15 C12.75 15.4142 12.4142 15.75 12 15.75 C11.5858 15.75 11.25 15.4142 11.25 15 V12.75 H9 C8.58579 12.75 8.25 12.4142 8.25 12 C8.25 11.5858 8.58579 11.25 9 11.25 H11.25 L11.25 9 C11.25 8.58579 11.5858 8.25 12 8.25Z
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
                // M 12 8.25
                moveTo(x = 12.0f, y = 8.25f)
                // C 12.4142 8.25 12.75 8.58579 12.75 9
                curveTo(
                    x1 = 12.4142f,
                    y1 = 8.25f,
                    x2 = 12.75f,
                    y2 = 8.58579f,
                    x3 = 12.75f,
                    y3 = 9.0f,
                )
                // V 11.25
                verticalLineTo(y = 11.25f)
                // H 15
                horizontalLineTo(x = 15.0f)
                // C 15.4142 11.25 15.75 11.5858 15.75 12
                curveTo(
                    x1 = 15.4142f,
                    y1 = 11.25f,
                    x2 = 15.75f,
                    y2 = 11.5858f,
                    x3 = 15.75f,
                    y3 = 12.0f,
                )
                // C 15.75 12.4142 15.4142 12.75 15 12.75
                curveTo(
                    x1 = 15.75f,
                    y1 = 12.4142f,
                    x2 = 15.4142f,
                    y2 = 12.75f,
                    x3 = 15.0f,
                    y3 = 12.75f,
                )
                // H 12.75
                horizontalLineTo(x = 12.75f)
                // L 12.75 15
                lineTo(x = 12.75f, y = 15.0f)
                // C 12.75 15.4142 12.4142 15.75 12 15.75
                curveTo(
                    x1 = 12.75f,
                    y1 = 15.4142f,
                    x2 = 12.4142f,
                    y2 = 15.75f,
                    x3 = 12.0f,
                    y3 = 15.75f,
                )
                // C 11.5858 15.75 11.25 15.4142 11.25 15
                curveTo(
                    x1 = 11.5858f,
                    y1 = 15.75f,
                    x2 = 11.25f,
                    y2 = 15.4142f,
                    x3 = 11.25f,
                    y3 = 15.0f,
                )
                // V 12.75
                verticalLineTo(y = 12.75f)
                // H 9
                horizontalLineTo(x = 9.0f)
                // C 8.58579 12.75 8.25 12.4142 8.25 12
                curveTo(
                    x1 = 8.58579f,
                    y1 = 12.75f,
                    x2 = 8.25f,
                    y2 = 12.4142f,
                    x3 = 8.25f,
                    y3 = 12.0f,
                )
                // C 8.25 11.5858 8.58579 11.25 9 11.25
                curveTo(
                    x1 = 8.25f,
                    y1 = 11.5858f,
                    x2 = 8.58579f,
                    y2 = 11.25f,
                    x3 = 9.0f,
                    y3 = 11.25f,
                )
                // H 11.25
                horizontalLineTo(x = 11.25f)
                // L 11.25 9
                lineTo(x = 11.25f, y = 9.0f)
                // C 11.25 8.58579 11.5858 8.25 12 8.25z
                curveTo(
                    x1 = 11.25f,
                    y1 = 8.58579f,
                    x2 = 11.5858f,
                    y2 = 8.25f,
                    x3 = 12.0f,
                    y3 = 8.25f,
                )
                close()
            }
        }.build().also { _solarPlus = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPlus: ImageVector? = null
