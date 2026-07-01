/*
 * Solar Bold "File Text" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Files/File%20Text.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarDocumentText: ImageVector
    get() {
        val current = _solarDocumentText
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarDocumentText",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M14 22 H10 C6.22876 22 4.34315 22 3.17157 20.8284 C2 19.6569 2 17.7712 2 14 V10 C2 6.22876 2 4.34315 3.17157 3.17157 C4.34315 2 6.23869 2 10.0298 2 C10.6358 2 11.1214 2 11.53 2.01666 C11.5166 2.09659 11.5095 2.17813 11.5092 2.26057 L11.5 5.09497 C11.4999 6.19207 11.4998 7.16164 11.6049 7.94316 C11.7188 8.79028 11.9803 9.63726 12.6716 10.3285 C13.3628 11.0198 14.2098 11.2813 15.0569 11.3952 C15.8385 11.5003 16.808 11.5002 17.9051 11.5001 L18 11.5001 H21.9574 C22 12.0344 22 12.6901 22 13.5629 V14 C22 17.7712 22 19.6569 20.8284 20.8284 C19.6569 22 17.7712 22 14 22Z M5.25 14.5 C5.25 14.0858 5.58579 13.75 6 13.75 H14 C14.4142 13.75 14.75 14.0858 14.75 14.5 C14.75 14.9142 14.4142 15.25 14 15.25 H6 C5.58579 15.25 5.25 14.9142 5.25 14.5Z M5.25 18 C5.25 17.5858 5.58579 17.25 6 17.25 H11.5 C11.9142 17.25 12.25 17.5858 12.25 18 C12.25 18.4142 11.9142 18.75 11.5 18.75 H6 C5.58579 18.75 5.25 18.4142 5.25 18Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 14 22
                moveTo(x = 14.0f, y = 22.0f)
                // H 10
                horizontalLineTo(x = 10.0f)
                // C 6.22876 22 4.34315 22 3.17157 20.8284
                curveTo(
                    x1 = 6.22876f,
                    y1 = 22.0f,
                    x2 = 4.34315f,
                    y2 = 22.0f,
                    x3 = 3.17157f,
                    y3 = 20.8284f,
                )
                // C 2 19.6569 2 17.7712 2 14
                curveTo(
                    x1 = 2.0f,
                    y1 = 19.6569f,
                    x2 = 2.0f,
                    y2 = 17.7712f,
                    x3 = 2.0f,
                    y3 = 14.0f,
                )
                // V 10
                verticalLineTo(y = 10.0f)
                // C 2 6.22876 2 4.34315 3.17157 3.17157
                curveTo(
                    x1 = 2.0f,
                    y1 = 6.22876f,
                    x2 = 2.0f,
                    y2 = 4.34315f,
                    x3 = 3.17157f,
                    y3 = 3.17157f,
                )
                // C 4.34315 2 6.23869 2 10.0298 2
                curveTo(
                    x1 = 4.34315f,
                    y1 = 2.0f,
                    x2 = 6.23869f,
                    y2 = 2.0f,
                    x3 = 10.0298f,
                    y3 = 2.0f,
                )
                // C 10.6358 2 11.1214 2 11.53 2.01666
                curveTo(
                    x1 = 10.6358f,
                    y1 = 2.0f,
                    x2 = 11.1214f,
                    y2 = 2.0f,
                    x3 = 11.53f,
                    y3 = 2.01666f,
                )
                // C 11.5166 2.09659 11.5095 2.17813 11.5092 2.26057
                curveTo(
                    x1 = 11.5166f,
                    y1 = 2.09659f,
                    x2 = 11.5095f,
                    y2 = 2.17813f,
                    x3 = 11.5092f,
                    y3 = 2.26057f,
                )
                // L 11.5 5.09497
                lineTo(x = 11.5f, y = 5.09497f)
                // C 11.4999 6.19207 11.4998 7.16164 11.6049 7.94316
                curveTo(
                    x1 = 11.4999f,
                    y1 = 6.19207f,
                    x2 = 11.4998f,
                    y2 = 7.16164f,
                    x3 = 11.6049f,
                    y3 = 7.94316f,
                )
                // C 11.7188 8.79028 11.9803 9.63726 12.6716 10.3285
                curveTo(
                    x1 = 11.7188f,
                    y1 = 8.79028f,
                    x2 = 11.9803f,
                    y2 = 9.63726f,
                    x3 = 12.6716f,
                    y3 = 10.3285f,
                )
                // C 13.3628 11.0198 14.2098 11.2813 15.0569 11.3952
                curveTo(
                    x1 = 13.3628f,
                    y1 = 11.0198f,
                    x2 = 14.2098f,
                    y2 = 11.2813f,
                    x3 = 15.0569f,
                    y3 = 11.3952f,
                )
                // C 15.8385 11.5003 16.808 11.5002 17.9051 11.5001
                curveTo(
                    x1 = 15.8385f,
                    y1 = 11.5003f,
                    x2 = 16.808f,
                    y2 = 11.5002f,
                    x3 = 17.9051f,
                    y3 = 11.5001f,
                )
                // L 18 11.5001
                lineTo(x = 18.0f, y = 11.5001f)
                // H 21.9574
                horizontalLineTo(x = 21.9574f)
                // C 22 12.0344 22 12.6901 22 13.5629
                curveTo(
                    x1 = 22.0f,
                    y1 = 12.0344f,
                    x2 = 22.0f,
                    y2 = 12.6901f,
                    x3 = 22.0f,
                    y3 = 13.5629f,
                )
                // V 14
                verticalLineTo(y = 14.0f)
                // C 22 17.7712 22 19.6569 20.8284 20.8284
                curveTo(
                    x1 = 22.0f,
                    y1 = 17.7712f,
                    x2 = 22.0f,
                    y2 = 19.6569f,
                    x3 = 20.8284f,
                    y3 = 20.8284f,
                )
                // C 19.6569 22 17.7712 22 14 22z
                curveTo(
                    x1 = 19.6569f,
                    y1 = 22.0f,
                    x2 = 17.7712f,
                    y2 = 22.0f,
                    x3 = 14.0f,
                    y3 = 22.0f,
                )
                close()
                // M 5.25 14.5
                moveTo(x = 5.25f, y = 14.5f)
                // C 5.25 14.0858 5.58579 13.75 6 13.75
                curveTo(
                    x1 = 5.25f,
                    y1 = 14.0858f,
                    x2 = 5.58579f,
                    y2 = 13.75f,
                    x3 = 6.0f,
                    y3 = 13.75f,
                )
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 14.4142 13.75 14.75 14.0858 14.75 14.5
                curveTo(
                    x1 = 14.4142f,
                    y1 = 13.75f,
                    x2 = 14.75f,
                    y2 = 14.0858f,
                    x3 = 14.75f,
                    y3 = 14.5f,
                )
                // C 14.75 14.9142 14.4142 15.25 14 15.25
                curveTo(
                    x1 = 14.75f,
                    y1 = 14.9142f,
                    x2 = 14.4142f,
                    y2 = 15.25f,
                    x3 = 14.0f,
                    y3 = 15.25f,
                )
                // H 6
                horizontalLineTo(x = 6.0f)
                // C 5.58579 15.25 5.25 14.9142 5.25 14.5z
                curveTo(
                    x1 = 5.58579f,
                    y1 = 15.25f,
                    x2 = 5.25f,
                    y2 = 14.9142f,
                    x3 = 5.25f,
                    y3 = 14.5f,
                )
                close()
                // M 5.25 18
                moveTo(x = 5.25f, y = 18.0f)
                // C 5.25 17.5858 5.58579 17.25 6 17.25
                curveTo(
                    x1 = 5.25f,
                    y1 = 17.5858f,
                    x2 = 5.58579f,
                    y2 = 17.25f,
                    x3 = 6.0f,
                    y3 = 17.25f,
                )
                // H 11.5
                horizontalLineTo(x = 11.5f)
                // C 11.9142 17.25 12.25 17.5858 12.25 18
                curveTo(
                    x1 = 11.9142f,
                    y1 = 17.25f,
                    x2 = 12.25f,
                    y2 = 17.5858f,
                    x3 = 12.25f,
                    y3 = 18.0f,
                )
                // C 12.25 18.4142 11.9142 18.75 11.5 18.75
                curveTo(
                    x1 = 12.25f,
                    y1 = 18.4142f,
                    x2 = 11.9142f,
                    y2 = 18.75f,
                    x3 = 11.5f,
                    y3 = 18.75f,
                )
                // H 6
                horizontalLineTo(x = 6.0f)
                // C 5.58579 18.75 5.25 18.4142 5.25 18z
                curveTo(
                    x1 = 5.58579f,
                    y1 = 18.75f,
                    x2 = 5.25f,
                    y2 = 18.4142f,
                    x3 = 5.25f,
                    y3 = 18.0f,
                )
                close()
            }
            // M19.3517 7.61665 L15.3929 4.05375 C14.2651 3.03868 13.7012 2.53114 13.0092 2.26562 L13 5.00011 C13 7.35713 13 8.53564 13.7322 9.26787 C14.4645 10.0001 15.643 10.0001 18 10.0001 H21.5801 C21.2175 9.29588 20.5684 8.71164 19.3517 7.61665Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 19.3517 7.61665
                moveTo(x = 19.3517f, y = 7.61665f)
                // L 15.3929 4.05375
                lineTo(x = 15.3929f, y = 4.05375f)
                // C 14.2651 3.03868 13.7012 2.53114 13.0092 2.26562
                curveTo(
                    x1 = 14.2651f,
                    y1 = 3.03868f,
                    x2 = 13.7012f,
                    y2 = 2.53114f,
                    x3 = 13.0092f,
                    y3 = 2.26562f,
                )
                // L 13 5.00011
                lineTo(x = 13.0f, y = 5.00011f)
                // C 13 7.35713 13 8.53564 13.7322 9.26787
                curveTo(
                    x1 = 13.0f,
                    y1 = 7.35713f,
                    x2 = 13.0f,
                    y2 = 8.53564f,
                    x3 = 13.7322f,
                    y3 = 9.26787f,
                )
                // C 14.4645 10.0001 15.643 10.0001 18 10.0001
                curveTo(
                    x1 = 14.4645f,
                    y1 = 10.0001f,
                    x2 = 15.643f,
                    y2 = 10.0001f,
                    x3 = 18.0f,
                    y3 = 10.0001f,
                )
                // H 21.5801
                horizontalLineTo(x = 21.5801f)
                // C 21.2175 9.29588 20.5684 8.71164 19.3517 7.61665z
                curveTo(
                    x1 = 21.2175f,
                    y1 = 9.29588f,
                    x2 = 20.5684f,
                    y2 = 8.71164f,
                    x3 = 19.3517f,
                    y3 = 7.61665f,
                )
                close()
            }
        }.build().also { _solarDocumentText = it }
    }

@Suppress("ObjectPropertyName")
private var _solarDocumentText: ImageVector? = null
