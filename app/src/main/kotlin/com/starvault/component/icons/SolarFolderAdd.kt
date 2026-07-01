/*
 * Solar Bold "Add Folder" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Folders/Add%20Folder.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarFolderAdd: ImageVector
    get() {
        val current = _solarFolderAdd
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarFolderAdd",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M2.06935 5.00839 C2 5.37595 2 5.81722 2 6.69975 V13.75 C2 17.5212 2 19.4069 3.17157 20.5784 C4.34315 21.75 6.22876 21.75 10 21.75 H14 C17.7712 21.75 19.6569 21.75 20.8284 20.5784 C22 19.4069 22 17.5212 22 13.75 V11.5479 C22 8.91554 22 7.59935 21.2305 6.74383 C21.1598 6.66514 21.0849 6.59024 21.0062 6.51946 C20.1506 5.75 18.8345 5.75 16.2021 5.75 H15.8284 C14.6747 5.75 14.0979 5.75 13.5604 5.59678 C13.2651 5.5126 12.9804 5.39471 12.7121 5.24543 C12.2237 4.97367 11.8158 4.56578 11 3.75 L10.4497 3.19975 C10.1763 2.92633 10.0396 2.78961 9.89594 2.67051 C9.27652 2.15704 8.51665 1.84229 7.71557 1.76738 C7.52976 1.75 7.33642 1.75 6.94975 1.75 C6.06722 1.75 5.62595 1.75 5.25839 1.81935 C3.64031 2.12464 2.37464 3.39031 2.06935 5.00839Z M12 11 C12.4142 11 12.75 11.3358 12.75 11.75 V13 H14 C14.4142 13 14.75 13.3358 14.75 13.75 C14.75 14.1642 14.4142 14.5 14 14.5 H12.75 V15.75 C12.75 16.1642 12.4142 16.5 12 16.5 C11.5858 16.5 11.25 16.1642 11.25 15.75 V14.5 H10 C9.58579 14.5 9.25 14.1642 9.25 13.75 C9.25 13.3358 9.58579 13 10 13 H11.25 V11.75 C11.25 11.3358 11.5858 11 12 11Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 2.06935 5.00839
                moveTo(x = 2.06935f, y = 5.00839f)
                // C 2 5.37595 2 5.81722 2 6.69975
                curveTo(
                    x1 = 2.0f,
                    y1 = 5.37595f,
                    x2 = 2.0f,
                    y2 = 5.81722f,
                    x3 = 2.0f,
                    y3 = 6.69975f,
                )
                // V 13.75
                verticalLineTo(y = 13.75f)
                // C 2 17.5212 2 19.4069 3.17157 20.5784
                curveTo(
                    x1 = 2.0f,
                    y1 = 17.5212f,
                    x2 = 2.0f,
                    y2 = 19.4069f,
                    x3 = 3.17157f,
                    y3 = 20.5784f,
                )
                // C 4.34315 21.75 6.22876 21.75 10 21.75
                curveTo(
                    x1 = 4.34315f,
                    y1 = 21.75f,
                    x2 = 6.22876f,
                    y2 = 21.75f,
                    x3 = 10.0f,
                    y3 = 21.75f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 17.7712 21.75 19.6569 21.75 20.8284 20.5784
                curveTo(
                    x1 = 17.7712f,
                    y1 = 21.75f,
                    x2 = 19.6569f,
                    y2 = 21.75f,
                    x3 = 20.8284f,
                    y3 = 20.5784f,
                )
                // C 22 19.4069 22 17.5212 22 13.75
                curveTo(
                    x1 = 22.0f,
                    y1 = 19.4069f,
                    x2 = 22.0f,
                    y2 = 17.5212f,
                    x3 = 22.0f,
                    y3 = 13.75f,
                )
                // V 11.5479
                verticalLineTo(y = 11.5479f)
                // C 22 8.91554 22 7.59935 21.2305 6.74383
                curveTo(
                    x1 = 22.0f,
                    y1 = 8.91554f,
                    x2 = 22.0f,
                    y2 = 7.59935f,
                    x3 = 21.2305f,
                    y3 = 6.74383f,
                )
                // C 21.1598 6.66514 21.0849 6.59024 21.0062 6.51946
                curveTo(
                    x1 = 21.1598f,
                    y1 = 6.66514f,
                    x2 = 21.0849f,
                    y2 = 6.59024f,
                    x3 = 21.0062f,
                    y3 = 6.51946f,
                )
                // C 20.1506 5.75 18.8345 5.75 16.2021 5.75
                curveTo(
                    x1 = 20.1506f,
                    y1 = 5.75f,
                    x2 = 18.8345f,
                    y2 = 5.75f,
                    x3 = 16.2021f,
                    y3 = 5.75f,
                )
                // H 15.8284
                horizontalLineTo(x = 15.8284f)
                // C 14.6747 5.75 14.0979 5.75 13.5604 5.59678
                curveTo(
                    x1 = 14.6747f,
                    y1 = 5.75f,
                    x2 = 14.0979f,
                    y2 = 5.75f,
                    x3 = 13.5604f,
                    y3 = 5.59678f,
                )
                // C 13.2651 5.5126 12.9804 5.39471 12.7121 5.24543
                curveTo(
                    x1 = 13.2651f,
                    y1 = 5.5126f,
                    x2 = 12.9804f,
                    y2 = 5.39471f,
                    x3 = 12.7121f,
                    y3 = 5.24543f,
                )
                // C 12.2237 4.97367 11.8158 4.56578 11 3.75
                curveTo(
                    x1 = 12.2237f,
                    y1 = 4.97367f,
                    x2 = 11.8158f,
                    y2 = 4.56578f,
                    x3 = 11.0f,
                    y3 = 3.75f,
                )
                // L 10.4497 3.19975
                lineTo(x = 10.4497f, y = 3.19975f)
                // C 10.1763 2.92633 10.0396 2.78961 9.89594 2.67051
                curveTo(
                    x1 = 10.1763f,
                    y1 = 2.92633f,
                    x2 = 10.0396f,
                    y2 = 2.78961f,
                    x3 = 9.89594f,
                    y3 = 2.67051f,
                )
                // C 9.27652 2.15704 8.51665 1.84229 7.71557 1.76738
                curveTo(
                    x1 = 9.27652f,
                    y1 = 2.15704f,
                    x2 = 8.51665f,
                    y2 = 1.84229f,
                    x3 = 7.71557f,
                    y3 = 1.76738f,
                )
                // C 7.52976 1.75 7.33642 1.75 6.94975 1.75
                curveTo(
                    x1 = 7.52976f,
                    y1 = 1.75f,
                    x2 = 7.33642f,
                    y2 = 1.75f,
                    x3 = 6.94975f,
                    y3 = 1.75f,
                )
                // C 6.06722 1.75 5.62595 1.75 5.25839 1.81935
                curveTo(
                    x1 = 6.06722f,
                    y1 = 1.75f,
                    x2 = 5.62595f,
                    y2 = 1.75f,
                    x3 = 5.25839f,
                    y3 = 1.81935f,
                )
                // C 3.64031 2.12464 2.37464 3.39031 2.06935 5.00839z
                curveTo(
                    x1 = 3.64031f,
                    y1 = 2.12464f,
                    x2 = 2.37464f,
                    y2 = 3.39031f,
                    x3 = 2.06935f,
                    y3 = 5.00839f,
                )
                close()
                // M 12 11
                moveTo(x = 12.0f, y = 11.0f)
                // C 12.4142 11 12.75 11.3358 12.75 11.75
                curveTo(
                    x1 = 12.4142f,
                    y1 = 11.0f,
                    x2 = 12.75f,
                    y2 = 11.3358f,
                    x3 = 12.75f,
                    y3 = 11.75f,
                )
                // V 13
                verticalLineTo(y = 13.0f)
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 14.4142 13 14.75 13.3358 14.75 13.75
                curveTo(
                    x1 = 14.4142f,
                    y1 = 13.0f,
                    x2 = 14.75f,
                    y2 = 13.3358f,
                    x3 = 14.75f,
                    y3 = 13.75f,
                )
                // C 14.75 14.1642 14.4142 14.5 14 14.5
                curveTo(
                    x1 = 14.75f,
                    y1 = 14.1642f,
                    x2 = 14.4142f,
                    y2 = 14.5f,
                    x3 = 14.0f,
                    y3 = 14.5f,
                )
                // H 12.75
                horizontalLineTo(x = 12.75f)
                // V 15.75
                verticalLineTo(y = 15.75f)
                // C 12.75 16.1642 12.4142 16.5 12 16.5
                curveTo(
                    x1 = 12.75f,
                    y1 = 16.1642f,
                    x2 = 12.4142f,
                    y2 = 16.5f,
                    x3 = 12.0f,
                    y3 = 16.5f,
                )
                // C 11.5858 16.5 11.25 16.1642 11.25 15.75
                curveTo(
                    x1 = 11.5858f,
                    y1 = 16.5f,
                    x2 = 11.25f,
                    y2 = 16.1642f,
                    x3 = 11.25f,
                    y3 = 15.75f,
                )
                // V 14.5
                verticalLineTo(y = 14.5f)
                // H 10
                horizontalLineTo(x = 10.0f)
                // C 9.58579 14.5 9.25 14.1642 9.25 13.75
                curveTo(
                    x1 = 9.58579f,
                    y1 = 14.5f,
                    x2 = 9.25f,
                    y2 = 14.1642f,
                    x3 = 9.25f,
                    y3 = 13.75f,
                )
                // C 9.25 13.3358 9.58579 13 10 13
                curveTo(
                    x1 = 9.25f,
                    y1 = 13.3358f,
                    x2 = 9.58579f,
                    y2 = 13.0f,
                    x3 = 10.0f,
                    y3 = 13.0f,
                )
                // H 11.25
                horizontalLineTo(x = 11.25f)
                // V 11.75
                verticalLineTo(y = 11.75f)
                // C 11.25 11.3358 11.5858 11 12 11z
                curveTo(
                    x1 = 11.25f,
                    y1 = 11.3358f,
                    x2 = 11.5858f,
                    y2 = 11.0f,
                    x3 = 12.0f,
                    y3 = 11.0f,
                )
                close()
            }
        }.build().also { _solarFolderAdd = it }
    }

@Suppress("ObjectPropertyName")
private var _solarFolderAdd: ImageVector? = null
