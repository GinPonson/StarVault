/*
 * Solar Bold "Pause" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Video,%20Audio,%20Sound/Pause.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPause: ImageVector
    get() {
        val current = _solarPause
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPause",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2 6 C2 4.11438 2 3.17157 2.58579 2.58579 C3.17157 2 4.11438 2 6 2 C7.88562 2 8.82843 2 9.41421 2.58579 C10 3.17157 10 4.11438 10 6 V18 C10 19.8856 10 20.8284 9.41421 21.4142 C8.82843 22 7.88562 22 6 22 C4.11438 22 3.17157 22 2.58579 21.4142 C2 20.8284 2 19.8856 2 18 V6Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 2 6
                moveTo(x = 2.0f, y = 6.0f)
                // C 2 4.11438 2 3.17157 2.58579 2.58579
                curveTo(
                    x1 = 2.0f,
                    y1 = 4.11438f,
                    x2 = 2.0f,
                    y2 = 3.17157f,
                    x3 = 2.58579f,
                    y3 = 2.58579f,
                )
                // C 3.17157 2 4.11438 2 6 2
                curveTo(
                    x1 = 3.17157f,
                    y1 = 2.0f,
                    x2 = 4.11438f,
                    y2 = 2.0f,
                    x3 = 6.0f,
                    y3 = 2.0f,
                )
                // C 7.88562 2 8.82843 2 9.41421 2.58579
                curveTo(
                    x1 = 7.88562f,
                    y1 = 2.0f,
                    x2 = 8.82843f,
                    y2 = 2.0f,
                    x3 = 9.41421f,
                    y3 = 2.58579f,
                )
                // C 10 3.17157 10 4.11438 10 6
                curveTo(
                    x1 = 10.0f,
                    y1 = 3.17157f,
                    x2 = 10.0f,
                    y2 = 4.11438f,
                    x3 = 10.0f,
                    y3 = 6.0f,
                )
                // V 18
                verticalLineTo(y = 18.0f)
                // C 10 19.8856 10 20.8284 9.41421 21.4142
                curveTo(
                    x1 = 10.0f,
                    y1 = 19.8856f,
                    x2 = 10.0f,
                    y2 = 20.8284f,
                    x3 = 9.41421f,
                    y3 = 21.4142f,
                )
                // C 8.82843 22 7.88562 22 6 22
                curveTo(
                    x1 = 8.82843f,
                    y1 = 22.0f,
                    x2 = 7.88562f,
                    y2 = 22.0f,
                    x3 = 6.0f,
                    y3 = 22.0f,
                )
                // C 4.11438 22 3.17157 22 2.58579 21.4142
                curveTo(
                    x1 = 4.11438f,
                    y1 = 22.0f,
                    x2 = 3.17157f,
                    y2 = 22.0f,
                    x3 = 2.58579f,
                    y3 = 21.4142f,
                )
                // C 2 20.8284 2 19.8856 2 18
                curveTo(
                    x1 = 2.0f,
                    y1 = 20.8284f,
                    x2 = 2.0f,
                    y2 = 19.8856f,
                    x3 = 2.0f,
                    y3 = 18.0f,
                )
                // V 6z
                verticalLineTo(y = 6.0f)
                close()
            }
            // M14 6 C14 4.11438 14 3.17157 14.5858 2.58579 C15.1716 2 16.1144 2 18 2 C19.8856 2 20.8284 2 21.4142 2.58579 C22 3.17157 22 4.11438 22 6 V18 C22 19.8856 22 20.8284 21.4142 21.4142 C20.8284 22 19.8856 22 18 22 C16.1144 22 15.1716 22 14.5858 21.4142 C14 20.8284 14 19.8856 14 18 V6Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 14 6
                moveTo(x = 14.0f, y = 6.0f)
                // C 14 4.11438 14 3.17157 14.5858 2.58579
                curveTo(
                    x1 = 14.0f,
                    y1 = 4.11438f,
                    x2 = 14.0f,
                    y2 = 3.17157f,
                    x3 = 14.5858f,
                    y3 = 2.58579f,
                )
                // C 15.1716 2 16.1144 2 18 2
                curveTo(
                    x1 = 15.1716f,
                    y1 = 2.0f,
                    x2 = 16.1144f,
                    y2 = 2.0f,
                    x3 = 18.0f,
                    y3 = 2.0f,
                )
                // C 19.8856 2 20.8284 2 21.4142 2.58579
                curveTo(
                    x1 = 19.8856f,
                    y1 = 2.0f,
                    x2 = 20.8284f,
                    y2 = 2.0f,
                    x3 = 21.4142f,
                    y3 = 2.58579f,
                )
                // C 22 3.17157 22 4.11438 22 6
                curveTo(
                    x1 = 22.0f,
                    y1 = 3.17157f,
                    x2 = 22.0f,
                    y2 = 4.11438f,
                    x3 = 22.0f,
                    y3 = 6.0f,
                )
                // V 18
                verticalLineTo(y = 18.0f)
                // C 22 19.8856 22 20.8284 21.4142 21.4142
                curveTo(
                    x1 = 22.0f,
                    y1 = 19.8856f,
                    x2 = 22.0f,
                    y2 = 20.8284f,
                    x3 = 21.4142f,
                    y3 = 21.4142f,
                )
                // C 20.8284 22 19.8856 22 18 22
                curveTo(
                    x1 = 20.8284f,
                    y1 = 22.0f,
                    x2 = 19.8856f,
                    y2 = 22.0f,
                    x3 = 18.0f,
                    y3 = 22.0f,
                )
                // C 16.1144 22 15.1716 22 14.5858 21.4142
                curveTo(
                    x1 = 16.1144f,
                    y1 = 22.0f,
                    x2 = 15.1716f,
                    y2 = 22.0f,
                    x3 = 14.5858f,
                    y3 = 21.4142f,
                )
                // C 14 20.8284 14 19.8856 14 18
                curveTo(
                    x1 = 14.0f,
                    y1 = 20.8284f,
                    x2 = 14.0f,
                    y2 = 19.8856f,
                    x3 = 14.0f,
                    y3 = 18.0f,
                )
                // V 6z
                verticalLineTo(y = 6.0f)
                close()
            }
        }.build().also { _solarPause = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPause: ImageVector? = null
