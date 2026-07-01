/*
 * Solar Bold "Export" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows%20Action/Export.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarShareOut: ImageVector
    get() {
        val current = _shareOut
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarShareOut",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M8.84467 7.90533 C9.13756 8.19822 9.61244 8.19822 9.90533 7.90533 L11.625 6.18566 L11.625 14.375 C11.625 14.7892 11.9608 15.125 12.375 15.125 C12.7892 15.125 13.125 14.7892 13.125 14.375 V6.18566 L14.8447 7.90533 C15.1376 8.19822 15.6124 8.19822 15.9053 7.90533 C16.1982 7.61244 16.1982 7.13756 15.9053 6.84467 L12.9053 3.84467 C12.6124 3.55178 12.1376 3.55178 11.8447 3.84467 L8.84467 6.84467 C8.55178 7.13756 8.55178 7.61244 8.84467 7.90533Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 8.84467 7.90533
                moveTo(x = 8.84467f, y = 7.90533f)
                // C 9.13756 8.19822 9.61244 8.19822 9.90533 7.90533
                curveTo(
                    x1 = 9.13756f,
                    y1 = 8.19822f,
                    x2 = 9.61244f,
                    y2 = 8.19822f,
                    x3 = 9.90533f,
                    y3 = 7.90533f,
                )
                // L 11.625 6.18566
                lineTo(x = 11.625f, y = 6.18566f)
                // L 11.625 14.375
                lineTo(x = 11.625f, y = 14.375f)
                // C 11.625 14.7892 11.9608 15.125 12.375 15.125
                curveTo(
                    x1 = 11.625f,
                    y1 = 14.7892f,
                    x2 = 11.9608f,
                    y2 = 15.125f,
                    x3 = 12.375f,
                    y3 = 15.125f,
                )
                // C 12.7892 15.125 13.125 14.7892 13.125 14.375
                curveTo(
                    x1 = 12.7892f,
                    y1 = 15.125f,
                    x2 = 13.125f,
                    y2 = 14.7892f,
                    x3 = 13.125f,
                    y3 = 14.375f,
                )
                // V 6.18566
                verticalLineTo(y = 6.18566f)
                // L 14.8447 7.90533
                lineTo(x = 14.8447f, y = 7.90533f)
                // C 15.1376 8.19822 15.6124 8.19822 15.9053 7.90533
                curveTo(
                    x1 = 15.1376f,
                    y1 = 8.19822f,
                    x2 = 15.6124f,
                    y2 = 8.19822f,
                    x3 = 15.9053f,
                    y3 = 7.90533f,
                )
                // C 16.1982 7.61244 16.1982 7.13756 15.9053 6.84467
                curveTo(
                    x1 = 16.1982f,
                    y1 = 7.61244f,
                    x2 = 16.1982f,
                    y2 = 7.13756f,
                    x3 = 15.9053f,
                    y3 = 6.84467f,
                )
                // L 12.9053 3.84467
                lineTo(x = 12.9053f, y = 3.84467f)
                // C 12.6124 3.55178 12.1376 3.55178 11.8447 3.84467
                curveTo(
                    x1 = 12.6124f,
                    y1 = 3.55178f,
                    x2 = 12.1376f,
                    y2 = 3.55178f,
                    x3 = 11.8447f,
                    y3 = 3.84467f,
                )
                // L 8.84467 6.84467
                lineTo(x = 8.84467f, y = 6.84467f)
                // C 8.55178 7.13756 8.55178 7.61244 8.84467 7.90533z
                curveTo(
                    x1 = 8.55178f,
                    y1 = 7.13756f,
                    x2 = 8.55178f,
                    y2 = 7.61244f,
                    x3 = 8.84467f,
                    y3 = 7.90533f,
                )
                close()
            }
            // M12.375 20.375 C16.7933 20.375 20.375 16.7933 20.375 12.375 L16.625 12.375 C15.6822 12.375 15.2108 12.375 14.9179 12.6679 C14.625 12.9608 14.625 13.4322 14.625 14.375 C14.625 15.6176 13.6176 16.625 12.375 16.625 C11.1324 16.625 10.125 15.6176 10.125 14.375 C10.125 13.4322 10.125 12.9608 9.83211 12.6679 C9.53921 12.375 9.06781 12.375 8.125 12.375 H4.375 C4.375 16.7933 7.95672 20.375 12.375 20.375Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12.375 20.375
                moveTo(x = 12.375f, y = 20.375f)
                // C 16.7933 20.375 20.375 16.7933 20.375 12.375
                curveTo(
                    x1 = 16.7933f,
                    y1 = 20.375f,
                    x2 = 20.375f,
                    y2 = 16.7933f,
                    x3 = 20.375f,
                    y3 = 12.375f,
                )
                // L 16.625 12.375
                lineTo(x = 16.625f, y = 12.375f)
                // C 15.6822 12.375 15.2108 12.375 14.9179 12.6679
                curveTo(
                    x1 = 15.6822f,
                    y1 = 12.375f,
                    x2 = 15.2108f,
                    y2 = 12.375f,
                    x3 = 14.9179f,
                    y3 = 12.6679f,
                )
                // C 14.625 12.9608 14.625 13.4322 14.625 14.375
                curveTo(
                    x1 = 14.625f,
                    y1 = 12.9608f,
                    x2 = 14.625f,
                    y2 = 13.4322f,
                    x3 = 14.625f,
                    y3 = 14.375f,
                )
                // C 14.625 15.6176 13.6176 16.625 12.375 16.625
                curveTo(
                    x1 = 14.625f,
                    y1 = 15.6176f,
                    x2 = 13.6176f,
                    y2 = 16.625f,
                    x3 = 12.375f,
                    y3 = 16.625f,
                )
                // C 11.1324 16.625 10.125 15.6176 10.125 14.375
                curveTo(
                    x1 = 11.1324f,
                    y1 = 16.625f,
                    x2 = 10.125f,
                    y2 = 15.6176f,
                    x3 = 10.125f,
                    y3 = 14.375f,
                )
                // C 10.125 13.4322 10.125 12.9608 9.83211 12.6679
                curveTo(
                    x1 = 10.125f,
                    y1 = 13.4322f,
                    x2 = 10.125f,
                    y2 = 12.9608f,
                    x3 = 9.83211f,
                    y3 = 12.6679f,
                )
                // C 9.53921 12.375 9.06781 12.375 8.125 12.375
                curveTo(
                    x1 = 9.53921f,
                    y1 = 12.375f,
                    x2 = 9.06781f,
                    y2 = 12.375f,
                    x3 = 8.125f,
                    y3 = 12.375f,
                )
                // H 4.375
                horizontalLineTo(x = 4.375f)
                // C 4.375 16.7933 7.95672 20.375 12.375 20.375z
                curveTo(
                    x1 = 4.375f,
                    y1 = 16.7933f,
                    x2 = 7.95672f,
                    y2 = 20.375f,
                    x3 = 12.375f,
                    y3 = 20.375f,
                )
                close()
            }
        }.build().also { _shareOut = it }
    }


@Suppress("ObjectPropertyName")
private var _shareOut: ImageVector? = null
