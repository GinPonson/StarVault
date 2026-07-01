/*
 * Solar Bold "Minimize Square" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows%20Action/Minimize%20Square.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarMinimize: ImageVector
    get() {
        val current = _solarMinimize
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarMinimize",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2 12 C2 7.28595 2 4.92893 3.46447 3.46447 C4.92893 2 7.28595 2 12 2 C16.134 2 18.4553 2 19.9517 2.98767 L14.75 8.18934 V6.25 C14.75 5.83579 14.4142 5.5 14 5.5 C13.5858 5.5 13.25 5.83579 13.25 6.25 V10 C13.25 10.4142 13.5858 10.75 14 10.75 H17.75 C18.1642 10.75 18.5 10.4142 18.5 10 C18.5 9.58579 18.1642 9.25 17.75 9.25 H15.8107 L21.0123 4.04832 C22 5.54466 22 7.866 22 12 C22 16.714 22 19.0711 20.5355 20.5355 C19.0711 22 16.714 22 12 22 C7.866 22 5.54466 22 4.04833 21.0123 L9.25 15.8107 V17.75 C9.25 18.1642 9.58579 18.5 10 18.5 C10.4142 18.5 10.75 18.1642 10.75 17.75 V14 C10.75 13.5858 10.4142 13.25 10 13.25 H6.25 C5.83579 13.25 5.5 13.5858 5.5 14 C5.5 14.4142 5.83579 14.75 6.25 14.75 H8.18934 L2.98767 19.9517 C2 18.4553 2 16.134 2 12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 2 12
                moveTo(x = 2.0f, y = 12.0f)
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
                // C 16.134 2 18.4553 2 19.9517 2.98767
                curveTo(
                    x1 = 16.134f,
                    y1 = 2.0f,
                    x2 = 18.4553f,
                    y2 = 2.0f,
                    x3 = 19.9517f,
                    y3 = 2.98767f,
                )
                // L 14.75 8.18934
                lineTo(x = 14.75f, y = 8.18934f)
                // V 6.25
                verticalLineTo(y = 6.25f)
                // C 14.75 5.83579 14.4142 5.5 14 5.5
                curveTo(
                    x1 = 14.75f,
                    y1 = 5.83579f,
                    x2 = 14.4142f,
                    y2 = 5.5f,
                    x3 = 14.0f,
                    y3 = 5.5f,
                )
                // C 13.5858 5.5 13.25 5.83579 13.25 6.25
                curveTo(
                    x1 = 13.5858f,
                    y1 = 5.5f,
                    x2 = 13.25f,
                    y2 = 5.83579f,
                    x3 = 13.25f,
                    y3 = 6.25f,
                )
                // V 10
                verticalLineTo(y = 10.0f)
                // C 13.25 10.4142 13.5858 10.75 14 10.75
                curveTo(
                    x1 = 13.25f,
                    y1 = 10.4142f,
                    x2 = 13.5858f,
                    y2 = 10.75f,
                    x3 = 14.0f,
                    y3 = 10.75f,
                )
                // H 17.75
                horizontalLineTo(x = 17.75f)
                // C 18.1642 10.75 18.5 10.4142 18.5 10
                curveTo(
                    x1 = 18.1642f,
                    y1 = 10.75f,
                    x2 = 18.5f,
                    y2 = 10.4142f,
                    x3 = 18.5f,
                    y3 = 10.0f,
                )
                // C 18.5 9.58579 18.1642 9.25 17.75 9.25
                curveTo(
                    x1 = 18.5f,
                    y1 = 9.58579f,
                    x2 = 18.1642f,
                    y2 = 9.25f,
                    x3 = 17.75f,
                    y3 = 9.25f,
                )
                // H 15.8107
                horizontalLineTo(x = 15.8107f)
                // L 21.0123 4.04832
                lineTo(x = 21.0123f, y = 4.04832f)
                // C 22 5.54466 22 7.866 22 12
                curveTo(
                    x1 = 22.0f,
                    y1 = 5.54466f,
                    x2 = 22.0f,
                    y2 = 7.866f,
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
                // C 19.0711 22 16.714 22 12 22
                curveTo(
                    x1 = 19.0711f,
                    y1 = 22.0f,
                    x2 = 16.714f,
                    y2 = 22.0f,
                    x3 = 12.0f,
                    y3 = 22.0f,
                )
                // C 7.866 22 5.54466 22 4.04833 21.0123
                curveTo(
                    x1 = 7.866f,
                    y1 = 22.0f,
                    x2 = 5.54466f,
                    y2 = 22.0f,
                    x3 = 4.04833f,
                    y3 = 21.0123f,
                )
                // L 9.25 15.8107
                lineTo(x = 9.25f, y = 15.8107f)
                // V 17.75
                verticalLineTo(y = 17.75f)
                // C 9.25 18.1642 9.58579 18.5 10 18.5
                curveTo(
                    x1 = 9.25f,
                    y1 = 18.1642f,
                    x2 = 9.58579f,
                    y2 = 18.5f,
                    x3 = 10.0f,
                    y3 = 18.5f,
                )
                // C 10.4142 18.5 10.75 18.1642 10.75 17.75
                curveTo(
                    x1 = 10.4142f,
                    y1 = 18.5f,
                    x2 = 10.75f,
                    y2 = 18.1642f,
                    x3 = 10.75f,
                    y3 = 17.75f,
                )
                // V 14
                verticalLineTo(y = 14.0f)
                // C 10.75 13.5858 10.4142 13.25 10 13.25
                curveTo(
                    x1 = 10.75f,
                    y1 = 13.5858f,
                    x2 = 10.4142f,
                    y2 = 13.25f,
                    x3 = 10.0f,
                    y3 = 13.25f,
                )
                // H 6.25
                horizontalLineTo(x = 6.25f)
                // C 5.83579 13.25 5.5 13.5858 5.5 14
                curveTo(
                    x1 = 5.83579f,
                    y1 = 13.25f,
                    x2 = 5.5f,
                    y2 = 13.5858f,
                    x3 = 5.5f,
                    y3 = 14.0f,
                )
                // C 5.5 14.4142 5.83579 14.75 6.25 14.75
                curveTo(
                    x1 = 5.5f,
                    y1 = 14.4142f,
                    x2 = 5.83579f,
                    y2 = 14.75f,
                    x3 = 6.25f,
                    y3 = 14.75f,
                )
                // H 8.18934
                horizontalLineTo(x = 8.18934f)
                // L 2.98767 19.9517
                lineTo(x = 2.98767f, y = 19.9517f)
                // C 2 18.4553 2 16.134 2 12z
                curveTo(
                    x1 = 2.0f,
                    y1 = 18.4553f,
                    x2 = 2.0f,
                    y2 = 16.134f,
                    x3 = 2.0f,
                    y3 = 12.0f,
                )
                close()
            }
        }.build().also { _solarMinimize = it }
    }


@Suppress("ObjectPropertyName")
private var _solarMinimize: ImageVector? = null
