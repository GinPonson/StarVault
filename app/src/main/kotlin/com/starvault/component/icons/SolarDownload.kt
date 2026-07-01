/*
 * Solar Bold "Download" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows%20Action/Download.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarDownload: ImageVector
    get() {
        val current = _solarDownload
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarDownload",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 1.25 C11.5858 1.25 11.25 1.58579 11.25 2 V12.9726 L9.56944 11.0119 C9.29988 10.6974 8.8264 10.661 8.51191 10.9306 C8.19741 11.2001 8.16099 11.6736 8.43056 11.9881 L11.4306 15.4881 C11.573 15.6543 11.7811 15.75 12 15.75 C12.2189 15.75 12.427 15.6543 12.5694 15.4881 L15.5694 11.9881 C15.839 11.6736 15.8026 11.2001 15.4881 10.9306 C15.1736 10.661 14.7001 10.6974 14.4306 11.0119 L12.75 12.9726 L12.75 2 C12.75 1.58579 12.4142 1.25 12 1.25Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 12 1.25
                moveTo(x = 12.0f, y = 1.25f)
                // C 11.5858 1.25 11.25 1.58579 11.25 2
                curveTo(
                    x1 = 11.5858f,
                    y1 = 1.25f,
                    x2 = 11.25f,
                    y2 = 1.58579f,
                    x3 = 11.25f,
                    y3 = 2.0f,
                )
                // V 12.9726
                verticalLineTo(y = 12.9726f)
                // L 9.56944 11.0119
                lineTo(x = 9.56944f, y = 11.0119f)
                // C 9.29988 10.6974 8.8264 10.661 8.51191 10.9306
                curveTo(
                    x1 = 9.29988f,
                    y1 = 10.6974f,
                    x2 = 8.8264f,
                    y2 = 10.661f,
                    x3 = 8.51191f,
                    y3 = 10.9306f,
                )
                // C 8.19741 11.2001 8.16099 11.6736 8.43056 11.9881
                curveTo(
                    x1 = 8.19741f,
                    y1 = 11.2001f,
                    x2 = 8.16099f,
                    y2 = 11.6736f,
                    x3 = 8.43056f,
                    y3 = 11.9881f,
                )
                // L 11.4306 15.4881
                lineTo(x = 11.4306f, y = 15.4881f)
                // C 11.573 15.6543 11.7811 15.75 12 15.75
                curveTo(
                    x1 = 11.573f,
                    y1 = 15.6543f,
                    x2 = 11.7811f,
                    y2 = 15.75f,
                    x3 = 12.0f,
                    y3 = 15.75f,
                )
                // C 12.2189 15.75 12.427 15.6543 12.5694 15.4881
                curveTo(
                    x1 = 12.2189f,
                    y1 = 15.75f,
                    x2 = 12.427f,
                    y2 = 15.6543f,
                    x3 = 12.5694f,
                    y3 = 15.4881f,
                )
                // L 15.5694 11.9881
                lineTo(x = 15.5694f, y = 11.9881f)
                // C 15.839 11.6736 15.8026 11.2001 15.4881 10.9306
                curveTo(
                    x1 = 15.839f,
                    y1 = 11.6736f,
                    x2 = 15.8026f,
                    y2 = 11.2001f,
                    x3 = 15.4881f,
                    y3 = 10.9306f,
                )
                // C 15.1736 10.661 14.7001 10.6974 14.4306 11.0119
                curveTo(
                    x1 = 15.1736f,
                    y1 = 10.661f,
                    x2 = 14.7001f,
                    y2 = 10.6974f,
                    x3 = 14.4306f,
                    y3 = 11.0119f,
                )
                // L 12.75 12.9726
                lineTo(x = 12.75f, y = 12.9726f)
                // L 12.75 2
                lineTo(x = 12.75f, y = 2.0f)
                // C 12.75 1.58579 12.4142 1.25 12 1.25z
                curveTo(
                    x1 = 12.75f,
                    y1 = 1.58579f,
                    x2 = 12.4142f,
                    y2 = 1.25f,
                    x3 = 12.0f,
                    y3 = 1.25f,
                )
                close()
            }
            // M14.25 9 V9.37828 C14.9836 9.11973 15.8312 9.2491 16.4642 9.79167 C17.4077 10.6004 17.517 12.0208 16.7083 12.9643 L13.7083 16.4643 C13.2808 16.963 12.6568 17.25 12 17.25 C11.3431 17.25 10.7191 16.963 10.2916 16.4643 L7.29163 12.9643 C6.48293 12.0208 6.5922 10.6004 7.53568 9.79167 C8.16868 9.2491 9.01637 9.11973 9.74996 9.37828 V9 H8 C5.17157 9 3.75736 9 2.87868 9.87868 C2 10.7574 2 12.1716 2 15 V16 C2 18.8284 2 20.2426 2.87868 21.1213 C3.75736 22 5.17157 22 7.99999 22 H16 C18.8284 22 20.2426 22 21.1213 21.1213 C22 20.2426 22 18.8284 22 16 V15 C22 12.1716 22 10.7574 21.1213 9.87868 C20.2426 9 18.8284 9 16 9 H14.25Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 14.25 9
                moveTo(x = 14.25f, y = 9.0f)
                // V 9.37828
                verticalLineTo(y = 9.37828f)
                // C 14.9836 9.11973 15.8312 9.2491 16.4642 9.79167
                curveTo(
                    x1 = 14.9836f,
                    y1 = 9.11973f,
                    x2 = 15.8312f,
                    y2 = 9.2491f,
                    x3 = 16.4642f,
                    y3 = 9.79167f,
                )
                // C 17.4077 10.6004 17.517 12.0208 16.7083 12.9643
                curveTo(
                    x1 = 17.4077f,
                    y1 = 10.6004f,
                    x2 = 17.517f,
                    y2 = 12.0208f,
                    x3 = 16.7083f,
                    y3 = 12.9643f,
                )
                // L 13.7083 16.4643
                lineTo(x = 13.7083f, y = 16.4643f)
                // C 13.2808 16.963 12.6568 17.25 12 17.25
                curveTo(
                    x1 = 13.2808f,
                    y1 = 16.963f,
                    x2 = 12.6568f,
                    y2 = 17.25f,
                    x3 = 12.0f,
                    y3 = 17.25f,
                )
                // C 11.3431 17.25 10.7191 16.963 10.2916 16.4643
                curveTo(
                    x1 = 11.3431f,
                    y1 = 17.25f,
                    x2 = 10.7191f,
                    y2 = 16.963f,
                    x3 = 10.2916f,
                    y3 = 16.4643f,
                )
                // L 7.29163 12.9643
                lineTo(x = 7.29163f, y = 12.9643f)
                // C 6.48293 12.0208 6.5922 10.6004 7.53568 9.79167
                curveTo(
                    x1 = 6.48293f,
                    y1 = 12.0208f,
                    x2 = 6.5922f,
                    y2 = 10.6004f,
                    x3 = 7.53568f,
                    y3 = 9.79167f,
                )
                // C 8.16868 9.2491 9.01637 9.11973 9.74996 9.37828
                curveTo(
                    x1 = 8.16868f,
                    y1 = 9.2491f,
                    x2 = 9.01637f,
                    y2 = 9.11973f,
                    x3 = 9.74996f,
                    y3 = 9.37828f,
                )
                // V 9
                verticalLineTo(y = 9.0f)
                // H 8
                horizontalLineTo(x = 8.0f)
                // C 5.17157 9 3.75736 9 2.87868 9.87868
                curveTo(
                    x1 = 5.17157f,
                    y1 = 9.0f,
                    x2 = 3.75736f,
                    y2 = 9.0f,
                    x3 = 2.87868f,
                    y3 = 9.87868f,
                )
                // C 2 10.7574 2 12.1716 2 15
                curveTo(
                    x1 = 2.0f,
                    y1 = 10.7574f,
                    x2 = 2.0f,
                    y2 = 12.1716f,
                    x3 = 2.0f,
                    y3 = 15.0f,
                )
                // V 16
                verticalLineTo(y = 16.0f)
                // C 2 18.8284 2 20.2426 2.87868 21.1213
                curveTo(
                    x1 = 2.0f,
                    y1 = 18.8284f,
                    x2 = 2.0f,
                    y2 = 20.2426f,
                    x3 = 2.87868f,
                    y3 = 21.1213f,
                )
                // C 3.75736 22 5.17157 22 7.99999 22
                curveTo(
                    x1 = 3.75736f,
                    y1 = 22.0f,
                    x2 = 5.17157f,
                    y2 = 22.0f,
                    x3 = 7.99999f,
                    y3 = 22.0f,
                )
                // H 16
                horizontalLineTo(x = 16.0f)
                // C 18.8284 22 20.2426 22 21.1213 21.1213
                curveTo(
                    x1 = 18.8284f,
                    y1 = 22.0f,
                    x2 = 20.2426f,
                    y2 = 22.0f,
                    x3 = 21.1213f,
                    y3 = 21.1213f,
                )
                // C 22 20.2426 22 18.8284 22 16
                curveTo(
                    x1 = 22.0f,
                    y1 = 20.2426f,
                    x2 = 22.0f,
                    y2 = 18.8284f,
                    x3 = 22.0f,
                    y3 = 16.0f,
                )
                // V 15
                verticalLineTo(y = 15.0f)
                // C 22 12.1716 22 10.7574 21.1213 9.87868
                curveTo(
                    x1 = 22.0f,
                    y1 = 12.1716f,
                    x2 = 22.0f,
                    y2 = 10.7574f,
                    x3 = 21.1213f,
                    y3 = 9.87868f,
                )
                // C 20.2426 9 18.8284 9 16 9
                curveTo(
                    x1 = 20.2426f,
                    y1 = 9.0f,
                    x2 = 18.8284f,
                    y2 = 9.0f,
                    x3 = 16.0f,
                    y3 = 9.0f,
                )
                // H 14.25z
                horizontalLineTo(x = 14.25f)
                close()
            }
        }.build().also { _solarDownload = it }
    }


@Suppress("ObjectPropertyName")
private var _solarDownload: ImageVector? = null
