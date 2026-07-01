/*
 * Solar Bold "Menu Dots" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Menu%20Dots.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarMore: ImageVector
    get() {
        val current = _solarMore
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarMore",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M7 12 C7 13.1046 6.10457 14 5 14 C3.89543 14 3 13.1046 3 12 C3 10.8954 3.89543 10 5 10 C6.10457 10 7 10.8954 7 12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 7 12
                moveTo(x = 7.0f, y = 12.0f)
                // C 7 13.1046 6.10457 14 5 14
                curveTo(
                    x1 = 7.0f,
                    y1 = 13.1046f,
                    x2 = 6.10457f,
                    y2 = 14.0f,
                    x3 = 5.0f,
                    y3 = 14.0f,
                )
                // C 3.89543 14 3 13.1046 3 12
                curveTo(
                    x1 = 3.89543f,
                    y1 = 14.0f,
                    x2 = 3.0f,
                    y2 = 13.1046f,
                    x3 = 3.0f,
                    y3 = 12.0f,
                )
                // C 3 10.8954 3.89543 10 5 10
                curveTo(
                    x1 = 3.0f,
                    y1 = 10.8954f,
                    x2 = 3.89543f,
                    y2 = 10.0f,
                    x3 = 5.0f,
                    y3 = 10.0f,
                )
                // C 6.10457 10 7 10.8954 7 12z
                curveTo(
                    x1 = 6.10457f,
                    y1 = 10.0f,
                    x2 = 7.0f,
                    y2 = 10.8954f,
                    x3 = 7.0f,
                    y3 = 12.0f,
                )
                close()
            }
            // M14 12 C14 13.1046 13.1046 14 12 14 C10.8954 14 10 13.1046 10 12 C10 10.8954 10.8954 10 12 10 C13.1046 10 14 10.8954 14 12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 14 12
                moveTo(x = 14.0f, y = 12.0f)
                // C 14 13.1046 13.1046 14 12 14
                curveTo(
                    x1 = 14.0f,
                    y1 = 13.1046f,
                    x2 = 13.1046f,
                    y2 = 14.0f,
                    x3 = 12.0f,
                    y3 = 14.0f,
                )
                // C 10.8954 14 10 13.1046 10 12
                curveTo(
                    x1 = 10.8954f,
                    y1 = 14.0f,
                    x2 = 10.0f,
                    y2 = 13.1046f,
                    x3 = 10.0f,
                    y3 = 12.0f,
                )
                // C 10 10.8954 10.8954 10 12 10
                curveTo(
                    x1 = 10.0f,
                    y1 = 10.8954f,
                    x2 = 10.8954f,
                    y2 = 10.0f,
                    x3 = 12.0f,
                    y3 = 10.0f,
                )
                // C 13.1046 10 14 10.8954 14 12z
                curveTo(
                    x1 = 13.1046f,
                    y1 = 10.0f,
                    x2 = 14.0f,
                    y2 = 10.8954f,
                    x3 = 14.0f,
                    y3 = 12.0f,
                )
                close()
            }
            // M21 12 C21 13.1046 20.1046 14 19 14 C17.8954 14 17 13.1046 17 12 C17 10.8954 17.8954 10 19 10 C20.1046 10 21 10.8954 21 12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 21 12
                moveTo(x = 21.0f, y = 12.0f)
                // C 21 13.1046 20.1046 14 19 14
                curveTo(
                    x1 = 21.0f,
                    y1 = 13.1046f,
                    x2 = 20.1046f,
                    y2 = 14.0f,
                    x3 = 19.0f,
                    y3 = 14.0f,
                )
                // C 17.8954 14 17 13.1046 17 12
                curveTo(
                    x1 = 17.8954f,
                    y1 = 14.0f,
                    x2 = 17.0f,
                    y2 = 13.1046f,
                    x3 = 17.0f,
                    y3 = 12.0f,
                )
                // C 17 10.8954 17.8954 10 19 10
                curveTo(
                    x1 = 17.0f,
                    y1 = 10.8954f,
                    x2 = 17.8954f,
                    y2 = 10.0f,
                    x3 = 19.0f,
                    y3 = 10.0f,
                )
                // C 20.1046 10 21 10.8954 21 12z
                curveTo(
                    x1 = 20.1046f,
                    y1 = 10.0f,
                    x2 = 21.0f,
                    y2 = 10.8954f,
                    x3 = 21.0f,
                    y3 = 12.0f,
                )
                close()
            }
        }.build().also { _solarMore = it }
    }


@Suppress("ObjectPropertyName")
private var _solarMore: ImageVector? = null
