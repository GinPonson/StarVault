/*
 * Solar Bold "Skip Next" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Video,%20Audio,%20Sound/Skip%20Next.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarNext: ImageVector
    get() {
        val current = _solarNext
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarNext",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M16.6598 14.6474 C18.4467 13.4935 18.4467 10.5065 16.6598 9.35258 L5.87083 2.38548 C4.13419 1.26402 2 2.72368 2 5.0329 V18.9671 C2 21.2763 4.13419 22.736 5.87083 21.6145 L16.6598 14.6474Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 16.6598 14.6474
                moveTo(x = 16.6598f, y = 14.6474f)
                // C 18.4467 13.4935 18.4467 10.5065 16.6598 9.35258
                curveTo(
                    x1 = 18.4467f,
                    y1 = 13.4935f,
                    x2 = 18.4467f,
                    y2 = 10.5065f,
                    x3 = 16.6598f,
                    y3 = 9.35258f,
                )
                // L 5.87083 2.38548
                lineTo(x = 5.87083f, y = 2.38548f)
                // C 4.13419 1.26402 2 2.72368 2 5.0329
                curveTo(
                    x1 = 4.13419f,
                    y1 = 1.26402f,
                    x2 = 2.0f,
                    y2 = 2.72368f,
                    x3 = 2.0f,
                    y3 = 5.0329f,
                )
                // V 18.9671
                verticalLineTo(y = 18.9671f)
                // C 2 21.2763 4.13419 22.736 5.87083 21.6145
                curveTo(
                    x1 = 2.0f,
                    y1 = 21.2763f,
                    x2 = 4.13419f,
                    y2 = 22.736f,
                    x3 = 5.87083f,
                    y3 = 21.6145f,
                )
                // L 16.6598 14.6474z
                lineTo(x = 16.6598f, y = 14.6474f)
                close()
            }
            // M22.75 5 C22.75 4.58579 22.4142 4.25 22 4.25 C21.5858 4.25 21.25 4.58579 21.25 5 V19 C21.25 19.4142 21.5858 19.75 22 19.75 C22.4142 19.75 22.75 19.4142 22.75 19 V5Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 22.75 5
                moveTo(x = 22.75f, y = 5.0f)
                // C 22.75 4.58579 22.4142 4.25 22 4.25
                curveTo(
                    x1 = 22.75f,
                    y1 = 4.58579f,
                    x2 = 22.4142f,
                    y2 = 4.25f,
                    x3 = 22.0f,
                    y3 = 4.25f,
                )
                // C 21.5858 4.25 21.25 4.58579 21.25 5
                curveTo(
                    x1 = 21.5858f,
                    y1 = 4.25f,
                    x2 = 21.25f,
                    y2 = 4.58579f,
                    x3 = 21.25f,
                    y3 = 5.0f,
                )
                // V 19
                verticalLineTo(y = 19.0f)
                // C 21.25 19.4142 21.5858 19.75 22 19.75
                curveTo(
                    x1 = 21.25f,
                    y1 = 19.4142f,
                    x2 = 21.5858f,
                    y2 = 19.75f,
                    x3 = 22.0f,
                    y3 = 19.75f,
                )
                // C 22.4142 19.75 22.75 19.4142 22.75 19
                curveTo(
                    x1 = 22.4142f,
                    y1 = 19.75f,
                    x2 = 22.75f,
                    y2 = 19.4142f,
                    x3 = 22.75f,
                    y3 = 19.0f,
                )
                // V 5z
                verticalLineTo(y = 5.0f)
                close()
            }
        }.build().also { _solarNext = it }
    }


@Suppress("ObjectPropertyName")
private var _solarNext: ImageVector? = null
