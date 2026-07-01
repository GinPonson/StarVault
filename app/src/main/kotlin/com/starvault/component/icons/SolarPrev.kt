/*
 * Solar Bold "Skip Previous" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Video,%20Audio,%20Sound/Skip%20Previous.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPrev: ImageVector
    get() {
        val current = _solarPrev
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPrev",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M8.09015 14.6474 C6.30328 13.4935 6.30328 10.5065 8.09015 9.35258 L18.8792 2.38548 C20.6158 1.26402 22.75 2.72368 22.75 5.0329 V18.9671 C22.75 21.2763 20.6158 22.736 18.8792 21.6145 L8.09015 14.6474Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 8.09015 14.6474
                moveTo(x = 8.09015f, y = 14.6474f)
                // C 6.30328 13.4935 6.30328 10.5065 8.09015 9.35258
                curveTo(
                    x1 = 6.30328f,
                    y1 = 13.4935f,
                    x2 = 6.30328f,
                    y2 = 10.5065f,
                    x3 = 8.09015f,
                    y3 = 9.35258f,
                )
                // L 18.8792 2.38548
                lineTo(x = 18.8792f, y = 2.38548f)
                // C 20.6158 1.26402 22.75 2.72368 22.75 5.0329
                curveTo(
                    x1 = 20.6158f,
                    y1 = 1.26402f,
                    x2 = 22.75f,
                    y2 = 2.72368f,
                    x3 = 22.75f,
                    y3 = 5.0329f,
                )
                // V 18.9671
                verticalLineTo(y = 18.9671f)
                // C 22.75 21.2763 20.6158 22.736 18.8792 21.6145
                curveTo(
                    x1 = 22.75f,
                    y1 = 21.2763f,
                    x2 = 20.6158f,
                    y2 = 22.736f,
                    x3 = 18.8792f,
                    y3 = 21.6145f,
                )
                // L 8.09015 14.6474z
                lineTo(x = 8.09015f, y = 14.6474f)
                close()
            }
            // M2 5 C2 4.58579 2.33579 4.25 2.75 4.25 C3.16421 4.25 3.5 4.58579 3.5 5 V19 C3.5 19.4142 3.16421 19.75 2.75 19.75 C2.33579 19.75 2 19.4142 2 19 V5Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 2 5
                moveTo(x = 2.0f, y = 5.0f)
                // C 2 4.58579 2.33579 4.25 2.75 4.25
                curveTo(
                    x1 = 2.0f,
                    y1 = 4.58579f,
                    x2 = 2.33579f,
                    y2 = 4.25f,
                    x3 = 2.75f,
                    y3 = 4.25f,
                )
                // C 3.16421 4.25 3.5 4.58579 3.5 5
                curveTo(
                    x1 = 3.16421f,
                    y1 = 4.25f,
                    x2 = 3.5f,
                    y2 = 4.58579f,
                    x3 = 3.5f,
                    y3 = 5.0f,
                )
                // V 19
                verticalLineTo(y = 19.0f)
                // C 3.5 19.4142 3.16421 19.75 2.75 19.75
                curveTo(
                    x1 = 3.5f,
                    y1 = 19.4142f,
                    x2 = 3.16421f,
                    y2 = 19.75f,
                    x3 = 2.75f,
                    y3 = 19.75f,
                )
                // C 2.33579 19.75 2 19.4142 2 19
                curveTo(
                    x1 = 2.33579f,
                    y1 = 19.75f,
                    x2 = 2.0f,
                    y2 = 19.4142f,
                    x3 = 2.0f,
                    y3 = 19.0f,
                )
                // V 5z
                verticalLineTo(y = 5.0f)
                close()
            }
        }.build().also { _solarPrev = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPrev: ImageVector? = null
