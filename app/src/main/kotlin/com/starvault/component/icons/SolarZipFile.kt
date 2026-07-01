/*
 * Solar Bold "ZIP File" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Files/ZIP%20File.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarZipFile: ImageVector
    get() {
        val current = _solarZipFile
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarZipFile",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M4.17157 3.17157 C3 4.34315 3 6.22876 3 10 V14 C3 17.7712 3 19.6569 4.17157 20.8284 C5.34315 22 7.22876 22 11 22 H13 C16.7712 22 18.6569 22 19.8284 20.8284 C21 19.6569 21 17.7712 21 14 V10 C21 6.22876 21 4.34315 19.8284 3.17157 C18.6569 2 16.7712 2 13 2 H12 V3 V4 H13.5 C13.9714 4 14.2071 4 14.3536 4.14645 C14.5 4.29289 14.5 4.5286 14.5 5 C14.5 5.4714 14.5 5.70711 14.3536 5.85355 C14.2071 6 13.9714 6 13.5 6 H12 V8 H13.5 C13.9714 8 14.2071 8 14.3536 8.14645 C14.5 8.29289 14.5 8.5286 14.5 9 C14.5 9.4714 14.5 9.70711 14.3536 9.85355 C14.2071 10 13.9714 10 13.5 10 H13 C12.5286 10 12.2929 10 12.1464 9.85355 C12 9.70711 12 9.4714 12 9 V8 H10.5 C10.0286 8 9.79289 8 9.64645 7.85355 C9.5 7.70711 9.5 7.4714 9.5 7 C9.5 6.5286 9.5 6.29289 9.64645 6.14645 C9.79289 6 10.0286 6 10.5 6 H12 V4 H10.5 C10.0286 4 9.79289 4 9.64645 3.85355 C9.5 3.70711 9.5 3.4714 9.5 3 V2.00338 C6.70613 2.02377 5.17628 2.16686 4.17157 3.17157Z M9.5 12.875 V13 C9.5 14.3807 10.6193 15.5 12 15.5 C13.3807 15.5 14.5 14.3807 14.5 13 V12.875 C14.5 12.3918 14.1082 12 13.625 12 H10.375 C9.89175 12 9.5 12.3918 9.5 12.875Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 4.17157 3.17157
                moveTo(x = 4.17157f, y = 3.17157f)
                // C 3 4.34315 3 6.22876 3 10
                curveTo(
                    x1 = 3.0f,
                    y1 = 4.34315f,
                    x2 = 3.0f,
                    y2 = 6.22876f,
                    x3 = 3.0f,
                    y3 = 10.0f,
                )
                // V 14
                verticalLineTo(y = 14.0f)
                // C 3 17.7712 3 19.6569 4.17157 20.8284
                curveTo(
                    x1 = 3.0f,
                    y1 = 17.7712f,
                    x2 = 3.0f,
                    y2 = 19.6569f,
                    x3 = 4.17157f,
                    y3 = 20.8284f,
                )
                // C 5.34315 22 7.22876 22 11 22
                curveTo(
                    x1 = 5.34315f,
                    y1 = 22.0f,
                    x2 = 7.22876f,
                    y2 = 22.0f,
                    x3 = 11.0f,
                    y3 = 22.0f,
                )
                // H 13
                horizontalLineTo(x = 13.0f)
                // C 16.7712 22 18.6569 22 19.8284 20.8284
                curveTo(
                    x1 = 16.7712f,
                    y1 = 22.0f,
                    x2 = 18.6569f,
                    y2 = 22.0f,
                    x3 = 19.8284f,
                    y3 = 20.8284f,
                )
                // C 21 19.6569 21 17.7712 21 14
                curveTo(
                    x1 = 21.0f,
                    y1 = 19.6569f,
                    x2 = 21.0f,
                    y2 = 17.7712f,
                    x3 = 21.0f,
                    y3 = 14.0f,
                )
                // V 10
                verticalLineTo(y = 10.0f)
                // C 21 6.22876 21 4.34315 19.8284 3.17157
                curveTo(
                    x1 = 21.0f,
                    y1 = 6.22876f,
                    x2 = 21.0f,
                    y2 = 4.34315f,
                    x3 = 19.8284f,
                    y3 = 3.17157f,
                )
                // C 18.6569 2 16.7712 2 13 2
                curveTo(
                    x1 = 18.6569f,
                    y1 = 2.0f,
                    x2 = 16.7712f,
                    y2 = 2.0f,
                    x3 = 13.0f,
                    y3 = 2.0f,
                )
                // H 12
                horizontalLineTo(x = 12.0f)
                // V 3
                verticalLineTo(y = 3.0f)
                // V 4
                verticalLineTo(y = 4.0f)
                // H 13.5
                horizontalLineTo(x = 13.5f)
                // C 13.9714 4 14.2071 4 14.3536 4.14645
                curveTo(
                    x1 = 13.9714f,
                    y1 = 4.0f,
                    x2 = 14.2071f,
                    y2 = 4.0f,
                    x3 = 14.3536f,
                    y3 = 4.14645f,
                )
                // C 14.5 4.29289 14.5 4.5286 14.5 5
                curveTo(
                    x1 = 14.5f,
                    y1 = 4.29289f,
                    x2 = 14.5f,
                    y2 = 4.5286f,
                    x3 = 14.5f,
                    y3 = 5.0f,
                )
                // C 14.5 5.4714 14.5 5.70711 14.3536 5.85355
                curveTo(
                    x1 = 14.5f,
                    y1 = 5.4714f,
                    x2 = 14.5f,
                    y2 = 5.70711f,
                    x3 = 14.3536f,
                    y3 = 5.85355f,
                )
                // C 14.2071 6 13.9714 6 13.5 6
                curveTo(
                    x1 = 14.2071f,
                    y1 = 6.0f,
                    x2 = 13.9714f,
                    y2 = 6.0f,
                    x3 = 13.5f,
                    y3 = 6.0f,
                )
                // H 12
                horizontalLineTo(x = 12.0f)
                // V 8
                verticalLineTo(y = 8.0f)
                // H 13.5
                horizontalLineTo(x = 13.5f)
                // C 13.9714 8 14.2071 8 14.3536 8.14645
                curveTo(
                    x1 = 13.9714f,
                    y1 = 8.0f,
                    x2 = 14.2071f,
                    y2 = 8.0f,
                    x3 = 14.3536f,
                    y3 = 8.14645f,
                )
                // C 14.5 8.29289 14.5 8.5286 14.5 9
                curveTo(
                    x1 = 14.5f,
                    y1 = 8.29289f,
                    x2 = 14.5f,
                    y2 = 8.5286f,
                    x3 = 14.5f,
                    y3 = 9.0f,
                )
                // C 14.5 9.4714 14.5 9.70711 14.3536 9.85355
                curveTo(
                    x1 = 14.5f,
                    y1 = 9.4714f,
                    x2 = 14.5f,
                    y2 = 9.70711f,
                    x3 = 14.3536f,
                    y3 = 9.85355f,
                )
                // C 14.2071 10 13.9714 10 13.5 10
                curveTo(
                    x1 = 14.2071f,
                    y1 = 10.0f,
                    x2 = 13.9714f,
                    y2 = 10.0f,
                    x3 = 13.5f,
                    y3 = 10.0f,
                )
                // H 13
                horizontalLineTo(x = 13.0f)
                // C 12.5286 10 12.2929 10 12.1464 9.85355
                curveTo(
                    x1 = 12.5286f,
                    y1 = 10.0f,
                    x2 = 12.2929f,
                    y2 = 10.0f,
                    x3 = 12.1464f,
                    y3 = 9.85355f,
                )
                // C 12 9.70711 12 9.4714 12 9
                curveTo(
                    x1 = 12.0f,
                    y1 = 9.70711f,
                    x2 = 12.0f,
                    y2 = 9.4714f,
                    x3 = 12.0f,
                    y3 = 9.0f,
                )
                // V 8
                verticalLineTo(y = 8.0f)
                // H 10.5
                horizontalLineTo(x = 10.5f)
                // C 10.0286 8 9.79289 8 9.64645 7.85355
                curveTo(
                    x1 = 10.0286f,
                    y1 = 8.0f,
                    x2 = 9.79289f,
                    y2 = 8.0f,
                    x3 = 9.64645f,
                    y3 = 7.85355f,
                )
                // C 9.5 7.70711 9.5 7.4714 9.5 7
                curveTo(
                    x1 = 9.5f,
                    y1 = 7.70711f,
                    x2 = 9.5f,
                    y2 = 7.4714f,
                    x3 = 9.5f,
                    y3 = 7.0f,
                )
                // C 9.5 6.5286 9.5 6.29289 9.64645 6.14645
                curveTo(
                    x1 = 9.5f,
                    y1 = 6.5286f,
                    x2 = 9.5f,
                    y2 = 6.29289f,
                    x3 = 9.64645f,
                    y3 = 6.14645f,
                )
                // C 9.79289 6 10.0286 6 10.5 6
                curveTo(
                    x1 = 9.79289f,
                    y1 = 6.0f,
                    x2 = 10.0286f,
                    y2 = 6.0f,
                    x3 = 10.5f,
                    y3 = 6.0f,
                )
                // H 12
                horizontalLineTo(x = 12.0f)
                // V 4
                verticalLineTo(y = 4.0f)
                // H 10.5
                horizontalLineTo(x = 10.5f)
                // C 10.0286 4 9.79289 4 9.64645 3.85355
                curveTo(
                    x1 = 10.0286f,
                    y1 = 4.0f,
                    x2 = 9.79289f,
                    y2 = 4.0f,
                    x3 = 9.64645f,
                    y3 = 3.85355f,
                )
                // C 9.5 3.70711 9.5 3.4714 9.5 3
                curveTo(
                    x1 = 9.5f,
                    y1 = 3.70711f,
                    x2 = 9.5f,
                    y2 = 3.4714f,
                    x3 = 9.5f,
                    y3 = 3.0f,
                )
                // V 2.00338
                verticalLineTo(y = 2.00338f)
                // C 6.70613 2.02377 5.17628 2.16686 4.17157 3.17157z
                curveTo(
                    x1 = 6.70613f,
                    y1 = 2.02377f,
                    x2 = 5.17628f,
                    y2 = 2.16686f,
                    x3 = 4.17157f,
                    y3 = 3.17157f,
                )
                close()
                // M 9.5 12.875
                moveTo(x = 9.5f, y = 12.875f)
                // V 13
                verticalLineTo(y = 13.0f)
                // C 9.5 14.3807 10.6193 15.5 12 15.5
                curveTo(
                    x1 = 9.5f,
                    y1 = 14.3807f,
                    x2 = 10.6193f,
                    y2 = 15.5f,
                    x3 = 12.0f,
                    y3 = 15.5f,
                )
                // C 13.3807 15.5 14.5 14.3807 14.5 13
                curveTo(
                    x1 = 13.3807f,
                    y1 = 15.5f,
                    x2 = 14.5f,
                    y2 = 14.3807f,
                    x3 = 14.5f,
                    y3 = 13.0f,
                )
                // V 12.875
                verticalLineTo(y = 12.875f)
                // C 14.5 12.3918 14.1082 12 13.625 12
                curveTo(
                    x1 = 14.5f,
                    y1 = 12.3918f,
                    x2 = 14.1082f,
                    y2 = 12.0f,
                    x3 = 13.625f,
                    y3 = 12.0f,
                )
                // H 10.375
                horizontalLineTo(x = 10.375f)
                // C 9.89175 12 9.5 12.3918 9.5 12.875z
                curveTo(
                    x1 = 9.89175f,
                    y1 = 12.0f,
                    x2 = 9.5f,
                    y2 = 12.3918f,
                    x3 = 9.5f,
                    y3 = 12.875f,
                )
                close()
            }
        }.build().also { _solarZipFile = it }
    }

@Suppress("ObjectPropertyName")
private var _solarZipFile: ImageVector? = null
