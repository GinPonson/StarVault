/*
 * Solar Bold "User" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Users/User.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarUser: ImageVector
    get() {
        val current = _solarUser
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarUser",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // <circle cx="12.0" cy="6.0" radius="4.0" fill="#FF000000" />
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12 6
                moveTo(x = 12.0f, y = 6.0f)
                // m -4 0
                moveToRelative(dx = -4.0f, dy = 0.0f)
                // a 4 4 0 1 1 8 0
                arcToRelative(
                    a = 4.0f,
                    b = 4.0f,
                    theta = 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = 8.0f,
                    dy1 = 0.0f,
                )
                // a 4 4 0 1 1 -8 0z
                arcToRelative(
                    a = 4.0f,
                    b = 4.0f,
                    theta = 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -8.0f,
                    dy1 = 0.0f,
                )
                close()
            }
            // M20 17.5 C20 19.9853 20 22 12 22 C4 22 4 19.9853 4 17.5 C4 15.0147 7.58172 13 12 13 C16.4183 13 20 15.0147 20 17.5Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 20 17.5
                moveTo(x = 20.0f, y = 17.5f)
                // C 20 19.9853 20 22 12 22
                curveTo(
                    x1 = 20.0f,
                    y1 = 19.9853f,
                    x2 = 20.0f,
                    y2 = 22.0f,
                    x3 = 12.0f,
                    y3 = 22.0f,
                )
                // C 4 22 4 19.9853 4 17.5
                curveTo(
                    x1 = 4.0f,
                    y1 = 22.0f,
                    x2 = 4.0f,
                    y2 = 19.9853f,
                    x3 = 4.0f,
                    y3 = 17.5f,
                )
                // C 4 15.0147 7.58172 13 12 13
                curveTo(
                    x1 = 4.0f,
                    y1 = 15.0147f,
                    x2 = 7.58172f,
                    y2 = 13.0f,
                    x3 = 12.0f,
                    y3 = 13.0f,
                )
                // C 16.4183 13 20 15.0147 20 17.5z
                curveTo(
                    x1 = 16.4183f,
                    y1 = 13.0f,
                    x2 = 20.0f,
                    y2 = 15.0147f,
                    x3 = 20.0f,
                    y3 = 17.5f,
                )
                close()
            }
        }.build().also { _solarUser = it }
    }

@Suppress("ObjectPropertyName")
private var _solarUser: ImageVector? = null
