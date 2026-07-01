/*
 * Solar Bold "Playlist" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/List/Playlist.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPlaylist: ImageVector
    get() {
        val current = _solarPlaylist
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPlaylist",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2.25 6 C2.25 5.58579 2.58579 5.25 3 5.25 H21 C21.4142 5.25 21.75 5.58579 21.75 6 C21.75 6.41421 21.4142 6.75 21 6.75 H3 C2.58579 6.75 2.25 6.41421 2.25 6Z M2.25 10 C2.25 9.58579 2.58579 9.25 3 9.25 H21 C21.4142 9.25 21.75 9.58579 21.75 10 C21.75 10.4142 21.4142 10.75 21 10.75 H3 C2.58579 10.75 2.25 10.4142 2.25 10Z M2.25 14 C2.25 13.5858 2.58579 13.25 3 13.25 H11 C11.4142 13.25 11.75 13.5858 11.75 14 C11.75 14.4142 11.4142 14.75 11 14.75 H3 C2.58579 14.75 2.25 14.4142 2.25 14Z M2.25 18 C2.25 17.5858 2.58579 17.25 3 17.25 H11 C11.4142 17.25 11.75 17.5858 11.75 18 C11.75 18.4142 11.4142 18.75 11 18.75 H3 C2.58579 18.75 2.25 18.4142 2.25 18Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 2.25 6
                moveTo(x = 2.25f, y = 6.0f)
                // C 2.25 5.58579 2.58579 5.25 3 5.25
                curveTo(
                    x1 = 2.25f,
                    y1 = 5.58579f,
                    x2 = 2.58579f,
                    y2 = 5.25f,
                    x3 = 3.0f,
                    y3 = 5.25f,
                )
                // H 21
                horizontalLineTo(x = 21.0f)
                // C 21.4142 5.25 21.75 5.58579 21.75 6
                curveTo(
                    x1 = 21.4142f,
                    y1 = 5.25f,
                    x2 = 21.75f,
                    y2 = 5.58579f,
                    x3 = 21.75f,
                    y3 = 6.0f,
                )
                // C 21.75 6.41421 21.4142 6.75 21 6.75
                curveTo(
                    x1 = 21.75f,
                    y1 = 6.41421f,
                    x2 = 21.4142f,
                    y2 = 6.75f,
                    x3 = 21.0f,
                    y3 = 6.75f,
                )
                // H 3
                horizontalLineTo(x = 3.0f)
                // C 2.58579 6.75 2.25 6.41421 2.25 6z
                curveTo(
                    x1 = 2.58579f,
                    y1 = 6.75f,
                    x2 = 2.25f,
                    y2 = 6.41421f,
                    x3 = 2.25f,
                    y3 = 6.0f,
                )
                close()
                // M 2.25 10
                moveTo(x = 2.25f, y = 10.0f)
                // C 2.25 9.58579 2.58579 9.25 3 9.25
                curveTo(
                    x1 = 2.25f,
                    y1 = 9.58579f,
                    x2 = 2.58579f,
                    y2 = 9.25f,
                    x3 = 3.0f,
                    y3 = 9.25f,
                )
                // H 21
                horizontalLineTo(x = 21.0f)
                // C 21.4142 9.25 21.75 9.58579 21.75 10
                curveTo(
                    x1 = 21.4142f,
                    y1 = 9.25f,
                    x2 = 21.75f,
                    y2 = 9.58579f,
                    x3 = 21.75f,
                    y3 = 10.0f,
                )
                // C 21.75 10.4142 21.4142 10.75 21 10.75
                curveTo(
                    x1 = 21.75f,
                    y1 = 10.4142f,
                    x2 = 21.4142f,
                    y2 = 10.75f,
                    x3 = 21.0f,
                    y3 = 10.75f,
                )
                // H 3
                horizontalLineTo(x = 3.0f)
                // C 2.58579 10.75 2.25 10.4142 2.25 10z
                curveTo(
                    x1 = 2.58579f,
                    y1 = 10.75f,
                    x2 = 2.25f,
                    y2 = 10.4142f,
                    x3 = 2.25f,
                    y3 = 10.0f,
                )
                close()
                // M 2.25 14
                moveTo(x = 2.25f, y = 14.0f)
                // C 2.25 13.5858 2.58579 13.25 3 13.25
                curveTo(
                    x1 = 2.25f,
                    y1 = 13.5858f,
                    x2 = 2.58579f,
                    y2 = 13.25f,
                    x3 = 3.0f,
                    y3 = 13.25f,
                )
                // H 11
                horizontalLineTo(x = 11.0f)
                // C 11.4142 13.25 11.75 13.5858 11.75 14
                curveTo(
                    x1 = 11.4142f,
                    y1 = 13.25f,
                    x2 = 11.75f,
                    y2 = 13.5858f,
                    x3 = 11.75f,
                    y3 = 14.0f,
                )
                // C 11.75 14.4142 11.4142 14.75 11 14.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 14.4142f,
                    x2 = 11.4142f,
                    y2 = 14.75f,
                    x3 = 11.0f,
                    y3 = 14.75f,
                )
                // H 3
                horizontalLineTo(x = 3.0f)
                // C 2.58579 14.75 2.25 14.4142 2.25 14z
                curveTo(
                    x1 = 2.58579f,
                    y1 = 14.75f,
                    x2 = 2.25f,
                    y2 = 14.4142f,
                    x3 = 2.25f,
                    y3 = 14.0f,
                )
                close()
                // M 2.25 18
                moveTo(x = 2.25f, y = 18.0f)
                // C 2.25 17.5858 2.58579 17.25 3 17.25
                curveTo(
                    x1 = 2.25f,
                    y1 = 17.5858f,
                    x2 = 2.58579f,
                    y2 = 17.25f,
                    x3 = 3.0f,
                    y3 = 17.25f,
                )
                // H 11
                horizontalLineTo(x = 11.0f)
                // C 11.4142 17.25 11.75 17.5858 11.75 18
                curveTo(
                    x1 = 11.4142f,
                    y1 = 17.25f,
                    x2 = 11.75f,
                    y2 = 17.5858f,
                    x3 = 11.75f,
                    y3 = 18.0f,
                )
                // C 11.75 18.4142 11.4142 18.75 11 18.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 18.4142f,
                    x2 = 11.4142f,
                    y2 = 18.75f,
                    x3 = 11.0f,
                    y3 = 18.75f,
                )
                // H 3
                horizontalLineTo(x = 3.0f)
                // C 2.58579 18.75 2.25 18.4142 2.25 18z
                curveTo(
                    x1 = 2.58579f,
                    y1 = 18.75f,
                    x2 = 2.25f,
                    y2 = 18.4142f,
                    x3 = 2.25f,
                    y3 = 18.0f,
                )
                close()
            }
            // M18.875 14.1184 C20.5288 15.0733 21.3558 15.5507 21.4772 16.2395 C21.5076 16.4118 21.5076 16.5882 21.4772 16.7605 C21.3558 17.4493 20.5288 17.9267 18.875 18.8816 C17.2212 19.8364 16.3942 20.3138 15.737 20.0746 C15.5725 20.0148 15.4199 19.9266 15.2858 19.8141 C14.75 19.3645 14.75 18.4097 14.75 16.5 C14.75 14.5903 14.75 13.6355 15.2858 13.1859 C15.4199 13.0734 15.5725 12.9852 15.737 12.9254 C16.3942 12.6862 17.2212 13.1636 18.875 14.1184Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 18.875 14.1184
                moveTo(x = 18.875f, y = 14.1184f)
                // C 20.5288 15.0733 21.3558 15.5507 21.4772 16.2395
                curveTo(
                    x1 = 20.5288f,
                    y1 = 15.0733f,
                    x2 = 21.3558f,
                    y2 = 15.5507f,
                    x3 = 21.4772f,
                    y3 = 16.2395f,
                )
                // C 21.5076 16.4118 21.5076 16.5882 21.4772 16.7605
                curveTo(
                    x1 = 21.5076f,
                    y1 = 16.4118f,
                    x2 = 21.5076f,
                    y2 = 16.5882f,
                    x3 = 21.4772f,
                    y3 = 16.7605f,
                )
                // C 21.3558 17.4493 20.5288 17.9267 18.875 18.8816
                curveTo(
                    x1 = 21.3558f,
                    y1 = 17.4493f,
                    x2 = 20.5288f,
                    y2 = 17.9267f,
                    x3 = 18.875f,
                    y3 = 18.8816f,
                )
                // C 17.2212 19.8364 16.3942 20.3138 15.737 20.0746
                curveTo(
                    x1 = 17.2212f,
                    y1 = 19.8364f,
                    x2 = 16.3942f,
                    y2 = 20.3138f,
                    x3 = 15.737f,
                    y3 = 20.0746f,
                )
                // C 15.5725 20.0148 15.4199 19.9266 15.2858 19.8141
                curveTo(
                    x1 = 15.5725f,
                    y1 = 20.0148f,
                    x2 = 15.4199f,
                    y2 = 19.9266f,
                    x3 = 15.2858f,
                    y3 = 19.8141f,
                )
                // C 14.75 19.3645 14.75 18.4097 14.75 16.5
                curveTo(
                    x1 = 14.75f,
                    y1 = 19.3645f,
                    x2 = 14.75f,
                    y2 = 18.4097f,
                    x3 = 14.75f,
                    y3 = 16.5f,
                )
                // C 14.75 14.5903 14.75 13.6355 15.2858 13.1859
                curveTo(
                    x1 = 14.75f,
                    y1 = 14.5903f,
                    x2 = 14.75f,
                    y2 = 13.6355f,
                    x3 = 15.2858f,
                    y3 = 13.1859f,
                )
                // C 15.4199 13.0734 15.5725 12.9852 15.737 12.9254
                curveTo(
                    x1 = 15.4199f,
                    y1 = 13.0734f,
                    x2 = 15.5725f,
                    y2 = 12.9852f,
                    x3 = 15.737f,
                    y3 = 12.9254f,
                )
                // C 16.3942 12.6862 17.2212 13.1636 18.875 14.1184z
                curveTo(
                    x1 = 16.3942f,
                    y1 = 12.6862f,
                    x2 = 17.2212f,
                    y2 = 13.1636f,
                    x3 = 18.875f,
                    y3 = 14.1184f,
                )
                close()
            }
        }.build().also { _solarPlaylist = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPlaylist: ImageVector? = null
