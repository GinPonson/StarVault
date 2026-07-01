/*
 * Solar Bold "Subtitles" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Subtitles.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarSubtitle: ImageVector
    get() {
        val current = _solarSubtitle
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarSubtitle",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2 12 C2 8.22876 2 6.34315 3.17157 5.17157 C4.34315 4 6.22876 4 10 4 H14 C17.7712 4 19.6569 4 20.8284 5.17157 C22 6.34315 22 8.22876 22 12 C22 15.7712 22 17.6569 20.8284 18.8284 C19.6569 20 17.7712 20 14 20 H10 C6.22876 20 4.34315 20 3.17157 18.8284 C2 17.6569 2 15.7712 2 12Z M6 15.25 C5.58579 15.25 5.25 15.5858 5.25 16 C5.25 16.4142 5.58579 16.75 6 16.75 H10 C10.4142 16.75 10.75 16.4142 10.75 16 C10.75 15.5858 10.4142 15.25 10 15.25 H6Z M7.75 13 C7.75 12.5858 7.41421 12.25 7 12.25 H6 C5.58579 12.25 5.25 12.5858 5.25 13 C5.25 13.4142 5.58579 13.75 6 13.75 H7 C7.41421 13.75 7.75 13.4142 7.75 13Z M11.5 12.25 C11.9142 12.25 12.25 12.5858 12.25 13 C12.25 13.4142 11.9142 13.75 11.5 13.75 H9.5 C9.08579 13.75 8.75 13.4142 8.75 13 C8.75 12.5858 9.08579 12.25 9.5 12.25 H11.5Z M18.75 13 C18.75 12.5858 18.4142 12.25 18 12.25 H14 C13.5858 12.25 13.25 12.5858 13.25 13 C13.25 13.4142 13.5858 13.75 14 13.75 H18 C18.4142 13.75 18.75 13.4142 18.75 13Z M12.5 15.25 C12.0858 15.25 11.75 15.5858 11.75 16 C11.75 16.4142 12.0858 16.75 12.5 16.75 H14 C14.4142 16.75 14.75 16.4142 14.75 16 C14.75 15.5858 14.4142 15.25 14 15.25 H12.5Z M15.75 16 C15.75 15.5858 16.0858 15.25 16.5 15.25 H18 C18.4142 15.25 18.75 15.5858 18.75 16 C18.75 16.4142 18.4142 16.75 18 16.75 H16.5 C16.0858 16.75 15.75 16.4142 15.75 16Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 2 12
                moveTo(x = 2.0f, y = 12.0f)
                // C 2 8.22876 2 6.34315 3.17157 5.17157
                curveTo(
                    x1 = 2.0f,
                    y1 = 8.22876f,
                    x2 = 2.0f,
                    y2 = 6.34315f,
                    x3 = 3.17157f,
                    y3 = 5.17157f,
                )
                // C 4.34315 4 6.22876 4 10 4
                curveTo(
                    x1 = 4.34315f,
                    y1 = 4.0f,
                    x2 = 6.22876f,
                    y2 = 4.0f,
                    x3 = 10.0f,
                    y3 = 4.0f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 17.7712 4 19.6569 4 20.8284 5.17157
                curveTo(
                    x1 = 17.7712f,
                    y1 = 4.0f,
                    x2 = 19.6569f,
                    y2 = 4.0f,
                    x3 = 20.8284f,
                    y3 = 5.17157f,
                )
                // C 22 6.34315 22 8.22876 22 12
                curveTo(
                    x1 = 22.0f,
                    y1 = 6.34315f,
                    x2 = 22.0f,
                    y2 = 8.22876f,
                    x3 = 22.0f,
                    y3 = 12.0f,
                )
                // C 22 15.7712 22 17.6569 20.8284 18.8284
                curveTo(
                    x1 = 22.0f,
                    y1 = 15.7712f,
                    x2 = 22.0f,
                    y2 = 17.6569f,
                    x3 = 20.8284f,
                    y3 = 18.8284f,
                )
                // C 19.6569 20 17.7712 20 14 20
                curveTo(
                    x1 = 19.6569f,
                    y1 = 20.0f,
                    x2 = 17.7712f,
                    y2 = 20.0f,
                    x3 = 14.0f,
                    y3 = 20.0f,
                )
                // H 10
                horizontalLineTo(x = 10.0f)
                // C 6.22876 20 4.34315 20 3.17157 18.8284
                curveTo(
                    x1 = 6.22876f,
                    y1 = 20.0f,
                    x2 = 4.34315f,
                    y2 = 20.0f,
                    x3 = 3.17157f,
                    y3 = 18.8284f,
                )
                // C 2 17.6569 2 15.7712 2 12z
                curveTo(
                    x1 = 2.0f,
                    y1 = 17.6569f,
                    x2 = 2.0f,
                    y2 = 15.7712f,
                    x3 = 2.0f,
                    y3 = 12.0f,
                )
                close()
                // M 6 15.25
                moveTo(x = 6.0f, y = 15.25f)
                // C 5.58579 15.25 5.25 15.5858 5.25 16
                curveTo(
                    x1 = 5.58579f,
                    y1 = 15.25f,
                    x2 = 5.25f,
                    y2 = 15.5858f,
                    x3 = 5.25f,
                    y3 = 16.0f,
                )
                // C 5.25 16.4142 5.58579 16.75 6 16.75
                curveTo(
                    x1 = 5.25f,
                    y1 = 16.4142f,
                    x2 = 5.58579f,
                    y2 = 16.75f,
                    x3 = 6.0f,
                    y3 = 16.75f,
                )
                // H 10
                horizontalLineTo(x = 10.0f)
                // C 10.4142 16.75 10.75 16.4142 10.75 16
                curveTo(
                    x1 = 10.4142f,
                    y1 = 16.75f,
                    x2 = 10.75f,
                    y2 = 16.4142f,
                    x3 = 10.75f,
                    y3 = 16.0f,
                )
                // C 10.75 15.5858 10.4142 15.25 10 15.25
                curveTo(
                    x1 = 10.75f,
                    y1 = 15.5858f,
                    x2 = 10.4142f,
                    y2 = 15.25f,
                    x3 = 10.0f,
                    y3 = 15.25f,
                )
                // H 6z
                horizontalLineTo(x = 6.0f)
                close()
                // M 7.75 13
                moveTo(x = 7.75f, y = 13.0f)
                // C 7.75 12.5858 7.41421 12.25 7 12.25
                curveTo(
                    x1 = 7.75f,
                    y1 = 12.5858f,
                    x2 = 7.41421f,
                    y2 = 12.25f,
                    x3 = 7.0f,
                    y3 = 12.25f,
                )
                // H 6
                horizontalLineTo(x = 6.0f)
                // C 5.58579 12.25 5.25 12.5858 5.25 13
                curveTo(
                    x1 = 5.58579f,
                    y1 = 12.25f,
                    x2 = 5.25f,
                    y2 = 12.5858f,
                    x3 = 5.25f,
                    y3 = 13.0f,
                )
                // C 5.25 13.4142 5.58579 13.75 6 13.75
                curveTo(
                    x1 = 5.25f,
                    y1 = 13.4142f,
                    x2 = 5.58579f,
                    y2 = 13.75f,
                    x3 = 6.0f,
                    y3 = 13.75f,
                )
                // H 7
                horizontalLineTo(x = 7.0f)
                // C 7.41421 13.75 7.75 13.4142 7.75 13z
                curveTo(
                    x1 = 7.41421f,
                    y1 = 13.75f,
                    x2 = 7.75f,
                    y2 = 13.4142f,
                    x3 = 7.75f,
                    y3 = 13.0f,
                )
                close()
                // M 11.5 12.25
                moveTo(x = 11.5f, y = 12.25f)
                // C 11.9142 12.25 12.25 12.5858 12.25 13
                curveTo(
                    x1 = 11.9142f,
                    y1 = 12.25f,
                    x2 = 12.25f,
                    y2 = 12.5858f,
                    x3 = 12.25f,
                    y3 = 13.0f,
                )
                // C 12.25 13.4142 11.9142 13.75 11.5 13.75
                curveTo(
                    x1 = 12.25f,
                    y1 = 13.4142f,
                    x2 = 11.9142f,
                    y2 = 13.75f,
                    x3 = 11.5f,
                    y3 = 13.75f,
                )
                // H 9.5
                horizontalLineTo(x = 9.5f)
                // C 9.08579 13.75 8.75 13.4142 8.75 13
                curveTo(
                    x1 = 9.08579f,
                    y1 = 13.75f,
                    x2 = 8.75f,
                    y2 = 13.4142f,
                    x3 = 8.75f,
                    y3 = 13.0f,
                )
                // C 8.75 12.5858 9.08579 12.25 9.5 12.25
                curveTo(
                    x1 = 8.75f,
                    y1 = 12.5858f,
                    x2 = 9.08579f,
                    y2 = 12.25f,
                    x3 = 9.5f,
                    y3 = 12.25f,
                )
                // H 11.5z
                horizontalLineTo(x = 11.5f)
                close()
                // M 18.75 13
                moveTo(x = 18.75f, y = 13.0f)
                // C 18.75 12.5858 18.4142 12.25 18 12.25
                curveTo(
                    x1 = 18.75f,
                    y1 = 12.5858f,
                    x2 = 18.4142f,
                    y2 = 12.25f,
                    x3 = 18.0f,
                    y3 = 12.25f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 13.5858 12.25 13.25 12.5858 13.25 13
                curveTo(
                    x1 = 13.5858f,
                    y1 = 12.25f,
                    x2 = 13.25f,
                    y2 = 12.5858f,
                    x3 = 13.25f,
                    y3 = 13.0f,
                )
                // C 13.25 13.4142 13.5858 13.75 14 13.75
                curveTo(
                    x1 = 13.25f,
                    y1 = 13.4142f,
                    x2 = 13.5858f,
                    y2 = 13.75f,
                    x3 = 14.0f,
                    y3 = 13.75f,
                )
                // H 18
                horizontalLineTo(x = 18.0f)
                // C 18.4142 13.75 18.75 13.4142 18.75 13z
                curveTo(
                    x1 = 18.4142f,
                    y1 = 13.75f,
                    x2 = 18.75f,
                    y2 = 13.4142f,
                    x3 = 18.75f,
                    y3 = 13.0f,
                )
                close()
                // M 12.5 15.25
                moveTo(x = 12.5f, y = 15.25f)
                // C 12.0858 15.25 11.75 15.5858 11.75 16
                curveTo(
                    x1 = 12.0858f,
                    y1 = 15.25f,
                    x2 = 11.75f,
                    y2 = 15.5858f,
                    x3 = 11.75f,
                    y3 = 16.0f,
                )
                // C 11.75 16.4142 12.0858 16.75 12.5 16.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 16.4142f,
                    x2 = 12.0858f,
                    y2 = 16.75f,
                    x3 = 12.5f,
                    y3 = 16.75f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 14.4142 16.75 14.75 16.4142 14.75 16
                curveTo(
                    x1 = 14.4142f,
                    y1 = 16.75f,
                    x2 = 14.75f,
                    y2 = 16.4142f,
                    x3 = 14.75f,
                    y3 = 16.0f,
                )
                // C 14.75 15.5858 14.4142 15.25 14 15.25
                curveTo(
                    x1 = 14.75f,
                    y1 = 15.5858f,
                    x2 = 14.4142f,
                    y2 = 15.25f,
                    x3 = 14.0f,
                    y3 = 15.25f,
                )
                // H 12.5z
                horizontalLineTo(x = 12.5f)
                close()
                // M 15.75 16
                moveTo(x = 15.75f, y = 16.0f)
                // C 15.75 15.5858 16.0858 15.25 16.5 15.25
                curveTo(
                    x1 = 15.75f,
                    y1 = 15.5858f,
                    x2 = 16.0858f,
                    y2 = 15.25f,
                    x3 = 16.5f,
                    y3 = 15.25f,
                )
                // H 18
                horizontalLineTo(x = 18.0f)
                // C 18.4142 15.25 18.75 15.5858 18.75 16
                curveTo(
                    x1 = 18.4142f,
                    y1 = 15.25f,
                    x2 = 18.75f,
                    y2 = 15.5858f,
                    x3 = 18.75f,
                    y3 = 16.0f,
                )
                // C 18.75 16.4142 18.4142 16.75 18 16.75
                curveTo(
                    x1 = 18.75f,
                    y1 = 16.4142f,
                    x2 = 18.4142f,
                    y2 = 16.75f,
                    x3 = 18.0f,
                    y3 = 16.75f,
                )
                // H 16.5
                horizontalLineTo(x = 16.5f)
                // C 16.0858 16.75 15.75 16.4142 15.75 16z
                curveTo(
                    x1 = 16.0858f,
                    y1 = 16.75f,
                    x2 = 15.75f,
                    y2 = 16.4142f,
                    x3 = 15.75f,
                    y3 = 16.0f,
                )
                close()
            }
        }.build().also { _solarSubtitle = it }
    }


@Suppress("ObjectPropertyName")
private var _solarSubtitle: ImageVector? = null
