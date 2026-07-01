/*
 * Solar Bold "Magnifer" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Search/Magnifer.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarSearch: ImageVector
    get() {
        val current = _solarSearch
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarSearch",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M21.7883 21.7883 C22.0706 21.506 22.0706 21.0483 21.7883 20.7659 L18.1224 17.1002 C19.4884 15.5007 20.3133 13.425 20.3133 11.1566 C20.3133 6.09956 16.2137 2 11.1566 2 C6.09956 2 2 6.09956 2 11.1566 C2 16.2137 6.09956 20.3133 11.1566 20.3133 C13.4249 20.3133 15.5006 19.4885 17.1 18.1225 L20.7659 21.7883 C21.0483 22.0706 21.506 22.0706 21.7883 21.7883Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 21.7883 21.7883
                moveTo(x = 21.7883f, y = 21.7883f)
                // C 22.0706 21.506 22.0706 21.0483 21.7883 20.7659
                curveTo(
                    x1 = 22.0706f,
                    y1 = 21.506f,
                    x2 = 22.0706f,
                    y2 = 21.0483f,
                    x3 = 21.7883f,
                    y3 = 20.7659f,
                )
                // L 18.1224 17.1002
                lineTo(x = 18.1224f, y = 17.1002f)
                // C 19.4884 15.5007 20.3133 13.425 20.3133 11.1566
                curveTo(
                    x1 = 19.4884f,
                    y1 = 15.5007f,
                    x2 = 20.3133f,
                    y2 = 13.425f,
                    x3 = 20.3133f,
                    y3 = 11.1566f,
                )
                // C 20.3133 6.09956 16.2137 2 11.1566 2
                curveTo(
                    x1 = 20.3133f,
                    y1 = 6.09956f,
                    x2 = 16.2137f,
                    y2 = 2.0f,
                    x3 = 11.1566f,
                    y3 = 2.0f,
                )
                // C 6.09956 2 2 6.09956 2 11.1566
                curveTo(
                    x1 = 6.09956f,
                    y1 = 2.0f,
                    x2 = 2.0f,
                    y2 = 6.09956f,
                    x3 = 2.0f,
                    y3 = 11.1566f,
                )
                // C 2 16.2137 6.09956 20.3133 11.1566 20.3133
                curveTo(
                    x1 = 2.0f,
                    y1 = 16.2137f,
                    x2 = 6.09956f,
                    y2 = 20.3133f,
                    x3 = 11.1566f,
                    y3 = 20.3133f,
                )
                // C 13.4249 20.3133 15.5006 19.4885 17.1 18.1225
                curveTo(
                    x1 = 13.4249f,
                    y1 = 20.3133f,
                    x2 = 15.5006f,
                    y2 = 19.4885f,
                    x3 = 17.1f,
                    y3 = 18.1225f,
                )
                // L 20.7659 21.7883
                lineTo(x = 20.7659f, y = 21.7883f)
                // C 21.0483 22.0706 21.506 22.0706 21.7883 21.7883z
                curveTo(
                    x1 = 21.0483f,
                    y1 = 22.0706f,
                    x2 = 21.506f,
                    y2 = 22.0706f,
                    x3 = 21.7883f,
                    y3 = 21.7883f,
                )
                close()
            }
        }.build().also { _solarSearch = it }
    }


@Suppress("ObjectPropertyName")
private var _solarSearch: ImageVector? = null
