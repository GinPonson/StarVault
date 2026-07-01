/*
 * Solar Bold "Folder" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Folders/Folder.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarFolder: ImageVector
    get() {
        val current = _solarFolder
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarFolder",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2.06935 5.25839 C2 5.62595 2 6.06722 2 6.94975 V14 C2 17.7712 2 19.6569 3.17157 20.8284 C4.34315 22 6.22876 22 10 22 H14 C17.7712 22 19.6569 22 20.8284 20.8284 C22 19.6569 22 17.7712 22 14 V11.7979 C22 9.16554 22 7.84935 21.2305 6.99383 C21.1598 6.91514 21.0849 6.84024 21.0062 6.76946 C20.1506 6 18.8345 6 16.2021 6 H15.8284 C14.6747 6 14.0979 6 13.5604 5.84678 C13.2651 5.7626 12.9804 5.64471 12.7121 5.49543 C12.2237 5.22367 11.8158 4.81578 11 4 L10.4497 3.44975 C10.1763 3.17633 10.0396 3.03961 9.89594 2.92051 C9.27652 2.40704 8.51665 2.09229 7.71557 2.01738 C7.52976 2 7.33642 2 6.94975 2 C6.06722 2 5.62595 2 5.25839 2.06935 C3.64031 2.37464 2.37464 3.64031 2.06935 5.25839Z M12.25 10 C12.25 9.58579 12.5858 9.25 13 9.25 H18 C18.4142 9.25 18.75 9.58579 18.75 10 C18.75 10.4142 18.4142 10.75 18 10.75 H13 C12.5858 10.75 12.25 10.4142 12.25 10Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 2.06935 5.25839
                moveTo(x = 2.06935f, y = 5.25839f)
                // C 2 5.62595 2 6.06722 2 6.94975
                curveTo(
                    x1 = 2.0f,
                    y1 = 5.62595f,
                    x2 = 2.0f,
                    y2 = 6.06722f,
                    x3 = 2.0f,
                    y3 = 6.94975f,
                )
                // V 14
                verticalLineTo(y = 14.0f)
                // C 2 17.7712 2 19.6569 3.17157 20.8284
                curveTo(
                    x1 = 2.0f,
                    y1 = 17.7712f,
                    x2 = 2.0f,
                    y2 = 19.6569f,
                    x3 = 3.17157f,
                    y3 = 20.8284f,
                )
                // C 4.34315 22 6.22876 22 10 22
                curveTo(
                    x1 = 4.34315f,
                    y1 = 22.0f,
                    x2 = 6.22876f,
                    y2 = 22.0f,
                    x3 = 10.0f,
                    y3 = 22.0f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 17.7712 22 19.6569 22 20.8284 20.8284
                curveTo(
                    x1 = 17.7712f,
                    y1 = 22.0f,
                    x2 = 19.6569f,
                    y2 = 22.0f,
                    x3 = 20.8284f,
                    y3 = 20.8284f,
                )
                // C 22 19.6569 22 17.7712 22 14
                curveTo(
                    x1 = 22.0f,
                    y1 = 19.6569f,
                    x2 = 22.0f,
                    y2 = 17.7712f,
                    x3 = 22.0f,
                    y3 = 14.0f,
                )
                // V 11.7979
                verticalLineTo(y = 11.7979f)
                // C 22 9.16554 22 7.84935 21.2305 6.99383
                curveTo(
                    x1 = 22.0f,
                    y1 = 9.16554f,
                    x2 = 22.0f,
                    y2 = 7.84935f,
                    x3 = 21.2305f,
                    y3 = 6.99383f,
                )
                // C 21.1598 6.91514 21.0849 6.84024 21.0062 6.76946
                curveTo(
                    x1 = 21.1598f,
                    y1 = 6.91514f,
                    x2 = 21.0849f,
                    y2 = 6.84024f,
                    x3 = 21.0062f,
                    y3 = 6.76946f,
                )
                // C 20.1506 6 18.8345 6 16.2021 6
                curveTo(
                    x1 = 20.1506f,
                    y1 = 6.0f,
                    x2 = 18.8345f,
                    y2 = 6.0f,
                    x3 = 16.2021f,
                    y3 = 6.0f,
                )
                // H 15.8284
                horizontalLineTo(x = 15.8284f)
                // C 14.6747 6 14.0979 6 13.5604 5.84678
                curveTo(
                    x1 = 14.6747f,
                    y1 = 6.0f,
                    x2 = 14.0979f,
                    y2 = 6.0f,
                    x3 = 13.5604f,
                    y3 = 5.84678f,
                )
                // C 13.2651 5.7626 12.9804 5.64471 12.7121 5.49543
                curveTo(
                    x1 = 13.2651f,
                    y1 = 5.7626f,
                    x2 = 12.9804f,
                    y2 = 5.64471f,
                    x3 = 12.7121f,
                    y3 = 5.49543f,
                )
                // C 12.2237 5.22367 11.8158 4.81578 11 4
                curveTo(
                    x1 = 12.2237f,
                    y1 = 5.22367f,
                    x2 = 11.8158f,
                    y2 = 4.81578f,
                    x3 = 11.0f,
                    y3 = 4.0f,
                )
                // L 10.4497 3.44975
                lineTo(x = 10.4497f, y = 3.44975f)
                // C 10.1763 3.17633 10.0396 3.03961 9.89594 2.92051
                curveTo(
                    x1 = 10.1763f,
                    y1 = 3.17633f,
                    x2 = 10.0396f,
                    y2 = 3.03961f,
                    x3 = 9.89594f,
                    y3 = 2.92051f,
                )
                // C 9.27652 2.40704 8.51665 2.09229 7.71557 2.01738
                curveTo(
                    x1 = 9.27652f,
                    y1 = 2.40704f,
                    x2 = 8.51665f,
                    y2 = 2.09229f,
                    x3 = 7.71557f,
                    y3 = 2.01738f,
                )
                // C 7.52976 2 7.33642 2 6.94975 2
                curveTo(
                    x1 = 7.52976f,
                    y1 = 2.0f,
                    x2 = 7.33642f,
                    y2 = 2.0f,
                    x3 = 6.94975f,
                    y3 = 2.0f,
                )
                // C 6.06722 2 5.62595 2 5.25839 2.06935
                curveTo(
                    x1 = 6.06722f,
                    y1 = 2.0f,
                    x2 = 5.62595f,
                    y2 = 2.0f,
                    x3 = 5.25839f,
                    y3 = 2.06935f,
                )
                // C 3.64031 2.37464 2.37464 3.64031 2.06935 5.25839z
                curveTo(
                    x1 = 3.64031f,
                    y1 = 2.37464f,
                    x2 = 2.37464f,
                    y2 = 3.64031f,
                    x3 = 2.06935f,
                    y3 = 5.25839f,
                )
                close()
                // M 12.25 10
                moveTo(x = 12.25f, y = 10.0f)
                // C 12.25 9.58579 12.5858 9.25 13 9.25
                curveTo(
                    x1 = 12.25f,
                    y1 = 9.58579f,
                    x2 = 12.5858f,
                    y2 = 9.25f,
                    x3 = 13.0f,
                    y3 = 9.25f,
                )
                // H 18
                horizontalLineTo(x = 18.0f)
                // C 18.4142 9.25 18.75 9.58579 18.75 10
                curveTo(
                    x1 = 18.4142f,
                    y1 = 9.25f,
                    x2 = 18.75f,
                    y2 = 9.58579f,
                    x3 = 18.75f,
                    y3 = 10.0f,
                )
                // C 18.75 10.4142 18.4142 10.75 18 10.75
                curveTo(
                    x1 = 18.75f,
                    y1 = 10.4142f,
                    x2 = 18.4142f,
                    y2 = 10.75f,
                    x3 = 18.0f,
                    y3 = 10.75f,
                )
                // H 13
                horizontalLineTo(x = 13.0f)
                // C 12.5858 10.75 12.25 10.4142 12.25 10z
                curveTo(
                    x1 = 12.5858f,
                    y1 = 10.75f,
                    x2 = 12.25f,
                    y2 = 10.4142f,
                    x3 = 12.25f,
                    y3 = 10.0f,
                )
                close()
            }
        }.build().also { _solarFolder = it }
    }

@Suppress("ObjectPropertyName")
private var _solarFolder: ImageVector? = null
