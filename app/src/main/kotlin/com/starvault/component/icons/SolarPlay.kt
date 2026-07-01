/*
 * Solar Bold "Play" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Video,%20Audio,%20Sound/Play.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPlay: ImageVector
    get() {
        val current = _solarPlay
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPlay",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M21.4086 9.35258 C23.5305 10.5065 23.5305 13.4935 21.4086 14.6474 L8.59662 21.6145 C6.53435 22.736 4 21.2763 4 18.9671 L4 5.0329 C4 2.72368 6.53435 1.26402 8.59661 2.38548 L21.4086 9.35258Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 21.4086 9.35258
                moveTo(x = 21.4086f, y = 9.35258f)
                // C 23.5305 10.5065 23.5305 13.4935 21.4086 14.6474
                curveTo(
                    x1 = 23.5305f,
                    y1 = 10.5065f,
                    x2 = 23.5305f,
                    y2 = 13.4935f,
                    x3 = 21.4086f,
                    y3 = 14.6474f,
                )
                // L 8.59662 21.6145
                lineTo(x = 8.59662f, y = 21.6145f)
                // C 6.53435 22.736 4 21.2763 4 18.9671
                curveTo(
                    x1 = 6.53435f,
                    y1 = 22.736f,
                    x2 = 4.0f,
                    y2 = 21.2763f,
                    x3 = 4.0f,
                    y3 = 18.9671f,
                )
                // L 4 5.0329
                lineTo(x = 4.0f, y = 5.0329f)
                // C 4 2.72368 6.53435 1.26402 8.59661 2.38548
                curveTo(
                    x1 = 4.0f,
                    y1 = 2.72368f,
                    x2 = 6.53435f,
                    y2 = 1.26402f,
                    x3 = 8.59661f,
                    y3 = 2.38548f,
                )
                // L 21.4086 9.35258z
                lineTo(x = 21.4086f, y = 9.35258f)
                close()
            }
        }.build().also { _solarPlay = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPlay: ImageVector? = null
