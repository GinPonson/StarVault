/*
 * Solar Bold "Clapperboard Play" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Video%2C%20Audio%2C%20Sound/Clapperboard%20Play.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarClapperboardPlay: ImageVector
    get() {
        val current = _solarClapperboardPlay
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarClapperboardPlay",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 2 C13.8452 2 15.3293 2 16.5401 2.08783 L13.0986 7.25002 H8.40139 L11.9014 2 H12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12 2
                moveTo(x = 12.0f, y = 2.0f)
                // C 13.8452 2 15.3293 2 16.5401 2.08783
                curveTo(
                    x1 = 13.8452f,
                    y1 = 2.0f,
                    x2 = 15.3293f,
                    y2 = 2.0f,
                    x3 = 16.5401f,
                    y3 = 2.08783f,
                )
                // L 13.0986 7.25002
                lineTo(x = 13.0986f, y = 7.25002f)
                // H 8.40139
                horizontalLineTo(x = 8.40139f)
                // L 11.9014 2
                lineTo(x = 11.9014f, y = 2.0f)
                // H 12z
                horizontalLineTo(x = 12.0f)
                close()
            }
            // M3.46447 3.46447 C4.71683 2.2121 6.62194 2.03072 10.0957 2.00445 L6.59861 7.25002 H2.10418 C2.25143 5.48593 2.6068 4.32213 3.46447 3.46447Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 3.46447 3.46447
                moveTo(x = 3.46447f, y = 3.46447f)
                // C 4.71683 2.2121 6.62194 2.03072 10.0957 2.00445
                curveTo(
                    x1 = 4.71683f,
                    y1 = 2.2121f,
                    x2 = 6.62194f,
                    y2 = 2.03072f,
                    x3 = 10.0957f,
                    y3 = 2.00445f,
                )
                // L 6.59861 7.25002
                lineTo(x = 6.59861f, y = 7.25002f)
                // H 2.10418
                horizontalLineTo(x = 2.10418f)
                // C 2.25143 5.48593 2.6068 4.32213 3.46447 3.46447z
                curveTo(
                    x1 = 2.25143f,
                    y1 = 5.48593f,
                    x2 = 2.6068f,
                    y2 = 4.32213f,
                    x3 = 3.46447f,
                    y3 = 3.46447f,
                )
                close()
            }
            // M2 12 C2 10.7633 2 9.68875 2.02644 8.75002 H21.9736 C22 9.68875 22 10.7633 22 12 C22 16.714 22 19.0711 20.5355 20.5355 C19.0711 22 16.714 22 12 22 C7.28595 22 4.92893 22 3.46447 20.5355 C2 19.0711 2 16.714 2 12Z M13.014 12.5852 C14.338 13.4395 15 13.8666 15 14.5 C15 15.1334 14.338 15.5605 13.014 16.4148 C11.6719 17.2807 11.0008 17.7137 10.5004 17.3958 C10 17.0779 10 16.2186 10 14.5 C10 12.7814 10 11.9221 10.5004 11.6042 C11.0008 11.2863 11.6719 11.7193 13.014 12.5852Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 2 12
                moveTo(x = 2.0f, y = 12.0f)
                // C 2 10.7633 2 9.68875 2.02644 8.75002
                curveTo(
                    x1 = 2.0f,
                    y1 = 10.7633f,
                    x2 = 2.0f,
                    y2 = 9.68875f,
                    x3 = 2.02644f,
                    y3 = 8.75002f,
                )
                // H 21.9736
                horizontalLineTo(x = 21.9736f)
                // C 22 9.68875 22 10.7633 22 12
                curveTo(
                    x1 = 22.0f,
                    y1 = 9.68875f,
                    x2 = 22.0f,
                    y2 = 10.7633f,
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
                // C 7.28595 22 4.92893 22 3.46447 20.5355
                curveTo(
                    x1 = 7.28595f,
                    y1 = 22.0f,
                    x2 = 4.92893f,
                    y2 = 22.0f,
                    x3 = 3.46447f,
                    y3 = 20.5355f,
                )
                // C 2 19.0711 2 16.714 2 12z
                curveTo(
                    x1 = 2.0f,
                    y1 = 19.0711f,
                    x2 = 2.0f,
                    y2 = 16.714f,
                    x3 = 2.0f,
                    y3 = 12.0f,
                )
                close()
                // M 13.014 12.5852
                moveTo(x = 13.014f, y = 12.5852f)
                // C 14.338 13.4395 15 13.8666 15 14.5
                curveTo(
                    x1 = 14.338f,
                    y1 = 13.4395f,
                    x2 = 15.0f,
                    y2 = 13.8666f,
                    x3 = 15.0f,
                    y3 = 14.5f,
                )
                // C 15 15.1334 14.338 15.5605 13.014 16.4148
                curveTo(
                    x1 = 15.0f,
                    y1 = 15.1334f,
                    x2 = 14.338f,
                    y2 = 15.5605f,
                    x3 = 13.014f,
                    y3 = 16.4148f,
                )
                // C 11.6719 17.2807 11.0008 17.7137 10.5004 17.3958
                curveTo(
                    x1 = 11.6719f,
                    y1 = 17.2807f,
                    x2 = 11.0008f,
                    y2 = 17.7137f,
                    x3 = 10.5004f,
                    y3 = 17.3958f,
                )
                // C 10 17.0779 10 16.2186 10 14.5
                curveTo(
                    x1 = 10.0f,
                    y1 = 17.0779f,
                    x2 = 10.0f,
                    y2 = 16.2186f,
                    x3 = 10.0f,
                    y3 = 14.5f,
                )
                // C 10 12.7814 10 11.9221 10.5004 11.6042
                curveTo(
                    x1 = 10.0f,
                    y1 = 12.7814f,
                    x2 = 10.0f,
                    y2 = 11.9221f,
                    x3 = 10.5004f,
                    y3 = 11.6042f,
                )
                // C 11.0008 11.2863 11.6719 11.7193 13.014 12.5852z
                curveTo(
                    x1 = 11.0008f,
                    y1 = 11.2863f,
                    x2 = 11.6719f,
                    y2 = 11.7193f,
                    x3 = 13.014f,
                    y3 = 12.5852f,
                )
                close()
            }
            // M21.8958 7.25002 C21.7486 5.48593 21.3932 4.32213 20.5355 3.46447 C19.9382 2.86714 19.1924 2.51345 18.1987 2.30403 L14.9014 7.25002 H21.8958Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 21.8958 7.25002
                moveTo(x = 21.8958f, y = 7.25002f)
                // C 21.7486 5.48593 21.3932 4.32213 20.5355 3.46447
                curveTo(
                    x1 = 21.7486f,
                    y1 = 5.48593f,
                    x2 = 21.3932f,
                    y2 = 4.32213f,
                    x3 = 20.5355f,
                    y3 = 3.46447f,
                )
                // C 19.9382 2.86714 19.1924 2.51345 18.1987 2.30403
                curveTo(
                    x1 = 19.9382f,
                    y1 = 2.86714f,
                    x2 = 19.1924f,
                    y2 = 2.51345f,
                    x3 = 18.1987f,
                    y3 = 2.30403f,
                )
                // L 14.9014 7.25002
                lineTo(x = 14.9014f, y = 7.25002f)
                // H 21.8958z
                horizontalLineTo(x = 21.8958f)
                close()
            }
        }.build().also { _solarClapperboardPlay = it }
    }

@Suppress("ObjectPropertyName")
private var _solarClapperboardPlay: ImageVector? = null
