/*
 * Solar Bold "List" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/List/List.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarListView: ImageVector
    get() {
        val current = _listView
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarListView",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M17 3.25 C17.2361 3.25 17.4584 3.36115 17.6 3.55 L20.6 7.55 C20.8485 7.88137 20.7814 8.35147 20.45 8.6 C20.1186 8.84853 19.6485 8.78137 19.4 8.45 L17.75 6.25 V17.75 L19.4 15.55 C19.6485 15.2186 20.1186 15.1515 20.45 15.4 C20.7814 15.6485 20.8485 16.1186 20.6 16.45 L17.6 20.45 C17.4584 20.6389 17.2361 20.75 17 20.75 C16.7639 20.75 16.5416 20.6389 16.4 20.45 L13.4 16.45 C13.1515 16.1186 13.2186 15.6485 13.55 15.4 C13.8814 15.1515 14.3515 15.2186 14.6 15.55 L16.25 17.75 V6.25 L14.6 8.45 C14.3515 8.78137 13.8814 8.84853 13.55 8.6 C13.2186 8.35147 13.1515 7.88137 13.4 7.55 L16.4 3.55 C16.5416 3.36115 16.7639 3.25 17 3.25Z M3.25 7 C3.25 6.58579 3.58579 6.25 4 6.25 H11 C11.4142 6.25 11.75 6.58579 11.75 7 C11.75 7.41421 11.4142 7.75 11 7.75 H4 C3.58579 7.75 3.25 7.41421 3.25 7Z M3.25 12 C3.25 11.5858 3.58579 11.25 4 11.25 H11 C11.4142 11.25 11.75 11.5858 11.75 12 C11.75 12.4142 11.4142 12.75 11 12.75 H4 C3.58579 12.75 3.25 12.4142 3.25 12Z M3.25 17 C3.25 16.5858 3.58579 16.25 4 16.25 H11 C11.4142 16.25 11.75 16.5858 11.75 17 C11.75 17.4142 11.4142 17.75 11 17.75 H4 C3.58579 17.75 3.25 17.4142 3.25 17Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 17 3.25
                moveTo(x = 17.0f, y = 3.25f)
                // C 17.2361 3.25 17.4584 3.36115 17.6 3.55
                curveTo(
                    x1 = 17.2361f,
                    y1 = 3.25f,
                    x2 = 17.4584f,
                    y2 = 3.36115f,
                    x3 = 17.6f,
                    y3 = 3.55f,
                )
                // L 20.6 7.55
                lineTo(x = 20.6f, y = 7.55f)
                // C 20.8485 7.88137 20.7814 8.35147 20.45 8.6
                curveTo(
                    x1 = 20.8485f,
                    y1 = 7.88137f,
                    x2 = 20.7814f,
                    y2 = 8.35147f,
                    x3 = 20.45f,
                    y3 = 8.6f,
                )
                // C 20.1186 8.84853 19.6485 8.78137 19.4 8.45
                curveTo(
                    x1 = 20.1186f,
                    y1 = 8.84853f,
                    x2 = 19.6485f,
                    y2 = 8.78137f,
                    x3 = 19.4f,
                    y3 = 8.45f,
                )
                // L 17.75 6.25
                lineTo(x = 17.75f, y = 6.25f)
                // V 17.75
                verticalLineTo(y = 17.75f)
                // L 19.4 15.55
                lineTo(x = 19.4f, y = 15.55f)
                // C 19.6485 15.2186 20.1186 15.1515 20.45 15.4
                curveTo(
                    x1 = 19.6485f,
                    y1 = 15.2186f,
                    x2 = 20.1186f,
                    y2 = 15.1515f,
                    x3 = 20.45f,
                    y3 = 15.4f,
                )
                // C 20.7814 15.6485 20.8485 16.1186 20.6 16.45
                curveTo(
                    x1 = 20.7814f,
                    y1 = 15.6485f,
                    x2 = 20.8485f,
                    y2 = 16.1186f,
                    x3 = 20.6f,
                    y3 = 16.45f,
                )
                // L 17.6 20.45
                lineTo(x = 17.6f, y = 20.45f)
                // C 17.4584 20.6389 17.2361 20.75 17 20.75
                curveTo(
                    x1 = 17.4584f,
                    y1 = 20.6389f,
                    x2 = 17.2361f,
                    y2 = 20.75f,
                    x3 = 17.0f,
                    y3 = 20.75f,
                )
                // C 16.7639 20.75 16.5416 20.6389 16.4 20.45
                curveTo(
                    x1 = 16.7639f,
                    y1 = 20.75f,
                    x2 = 16.5416f,
                    y2 = 20.6389f,
                    x3 = 16.4f,
                    y3 = 20.45f,
                )
                // L 13.4 16.45
                lineTo(x = 13.4f, y = 16.45f)
                // C 13.1515 16.1186 13.2186 15.6485 13.55 15.4
                curveTo(
                    x1 = 13.1515f,
                    y1 = 16.1186f,
                    x2 = 13.2186f,
                    y2 = 15.6485f,
                    x3 = 13.55f,
                    y3 = 15.4f,
                )
                // C 13.8814 15.1515 14.3515 15.2186 14.6 15.55
                curveTo(
                    x1 = 13.8814f,
                    y1 = 15.1515f,
                    x2 = 14.3515f,
                    y2 = 15.2186f,
                    x3 = 14.6f,
                    y3 = 15.55f,
                )
                // L 16.25 17.75
                lineTo(x = 16.25f, y = 17.75f)
                // V 6.25
                verticalLineTo(y = 6.25f)
                // L 14.6 8.45
                lineTo(x = 14.6f, y = 8.45f)
                // C 14.3515 8.78137 13.8814 8.84853 13.55 8.6
                curveTo(
                    x1 = 14.3515f,
                    y1 = 8.78137f,
                    x2 = 13.8814f,
                    y2 = 8.84853f,
                    x3 = 13.55f,
                    y3 = 8.6f,
                )
                // C 13.2186 8.35147 13.1515 7.88137 13.4 7.55
                curveTo(
                    x1 = 13.2186f,
                    y1 = 8.35147f,
                    x2 = 13.1515f,
                    y2 = 7.88137f,
                    x3 = 13.4f,
                    y3 = 7.55f,
                )
                // L 16.4 3.55
                lineTo(x = 16.4f, y = 3.55f)
                // C 16.5416 3.36115 16.7639 3.25 17 3.25z
                curveTo(
                    x1 = 16.5416f,
                    y1 = 3.36115f,
                    x2 = 16.7639f,
                    y2 = 3.25f,
                    x3 = 17.0f,
                    y3 = 3.25f,
                )
                close()
                // M 3.25 7
                moveTo(x = 3.25f, y = 7.0f)
                // C 3.25 6.58579 3.58579 6.25 4 6.25
                curveTo(
                    x1 = 3.25f,
                    y1 = 6.58579f,
                    x2 = 3.58579f,
                    y2 = 6.25f,
                    x3 = 4.0f,
                    y3 = 6.25f,
                )
                // H 11
                horizontalLineTo(x = 11.0f)
                // C 11.4142 6.25 11.75 6.58579 11.75 7
                curveTo(
                    x1 = 11.4142f,
                    y1 = 6.25f,
                    x2 = 11.75f,
                    y2 = 6.58579f,
                    x3 = 11.75f,
                    y3 = 7.0f,
                )
                // C 11.75 7.41421 11.4142 7.75 11 7.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 7.41421f,
                    x2 = 11.4142f,
                    y2 = 7.75f,
                    x3 = 11.0f,
                    y3 = 7.75f,
                )
                // H 4
                horizontalLineTo(x = 4.0f)
                // C 3.58579 7.75 3.25 7.41421 3.25 7z
                curveTo(
                    x1 = 3.58579f,
                    y1 = 7.75f,
                    x2 = 3.25f,
                    y2 = 7.41421f,
                    x3 = 3.25f,
                    y3 = 7.0f,
                )
                close()
                // M 3.25 12
                moveTo(x = 3.25f, y = 12.0f)
                // C 3.25 11.5858 3.58579 11.25 4 11.25
                curveTo(
                    x1 = 3.25f,
                    y1 = 11.5858f,
                    x2 = 3.58579f,
                    y2 = 11.25f,
                    x3 = 4.0f,
                    y3 = 11.25f,
                )
                // H 11
                horizontalLineTo(x = 11.0f)
                // C 11.4142 11.25 11.75 11.5858 11.75 12
                curveTo(
                    x1 = 11.4142f,
                    y1 = 11.25f,
                    x2 = 11.75f,
                    y2 = 11.5858f,
                    x3 = 11.75f,
                    y3 = 12.0f,
                )
                // C 11.75 12.4142 11.4142 12.75 11 12.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 12.4142f,
                    x2 = 11.4142f,
                    y2 = 12.75f,
                    x3 = 11.0f,
                    y3 = 12.75f,
                )
                // H 4
                horizontalLineTo(x = 4.0f)
                // C 3.58579 12.75 3.25 12.4142 3.25 12z
                curveTo(
                    x1 = 3.58579f,
                    y1 = 12.75f,
                    x2 = 3.25f,
                    y2 = 12.4142f,
                    x3 = 3.25f,
                    y3 = 12.0f,
                )
                close()
                // M 3.25 17
                moveTo(x = 3.25f, y = 17.0f)
                // C 3.25 16.5858 3.58579 16.25 4 16.25
                curveTo(
                    x1 = 3.25f,
                    y1 = 16.5858f,
                    x2 = 3.58579f,
                    y2 = 16.25f,
                    x3 = 4.0f,
                    y3 = 16.25f,
                )
                // H 11
                horizontalLineTo(x = 11.0f)
                // C 11.4142 16.25 11.75 16.5858 11.75 17
                curveTo(
                    x1 = 11.4142f,
                    y1 = 16.25f,
                    x2 = 11.75f,
                    y2 = 16.5858f,
                    x3 = 11.75f,
                    y3 = 17.0f,
                )
                // C 11.75 17.4142 11.4142 17.75 11 17.75
                curveTo(
                    x1 = 11.75f,
                    y1 = 17.4142f,
                    x2 = 11.4142f,
                    y2 = 17.75f,
                    x3 = 11.0f,
                    y3 = 17.75f,
                )
                // H 4
                horizontalLineTo(x = 4.0f)
                // C 3.58579 17.75 3.25 17.4142 3.25 17z
                curveTo(
                    x1 = 3.58579f,
                    y1 = 17.75f,
                    x2 = 3.25f,
                    y2 = 17.4142f,
                    x3 = 3.25f,
                    y3 = 17.0f,
                )
                close()
            }
        }.build().also { _listView = it }
    }


@Suppress("ObjectPropertyName")
private var _listView: ImageVector? = null
