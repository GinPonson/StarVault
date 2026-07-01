/*
 * Solar Bold "Shield" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Security/Shield.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarPrivacy: ImageVector
    get() {
        val current = _solarPrivacy
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarPrivacy",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M11.25 2.07342 C10.6437 2.18652 9.93159 2.43028 8.83772 2.80472 L8.26491 3.00079 C5.25832 4.02996 3.75503 4.54454 3.37752 5.08241 C3.00825 5.60853 3.00018 7.14974 3 10.2094 L11.25 7.45943 V2.07342Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 11.25 2.07342
                moveTo(x = 11.25f, y = 2.07342f)
                // C 10.6437 2.18652 9.93159 2.43028 8.83772 2.80472
                curveTo(
                    x1 = 10.6437f,
                    y1 = 2.18652f,
                    x2 = 9.93159f,
                    y2 = 2.43028f,
                    x3 = 8.83772f,
                    y3 = 2.80472f,
                )
                // L 8.26491 3.00079
                lineTo(x = 8.26491f, y = 3.00079f)
                // C 5.25832 4.02996 3.75503 4.54454 3.37752 5.08241
                curveTo(
                    x1 = 5.25832f,
                    y1 = 4.02996f,
                    x2 = 3.75503f,
                    y2 = 4.54454f,
                    x3 = 3.37752f,
                    y3 = 5.08241f,
                )
                // C 3.00825 5.60853 3.00018 7.14974 3 10.2094
                curveTo(
                    x1 = 3.00825f,
                    y1 = 5.60853f,
                    x2 = 3.00018f,
                    y2 = 7.14974f,
                    x3 = 3.0f,
                    y3 = 10.2094f,
                )
                // L 11.25 7.45943
                lineTo(x = 11.25f, y = 7.45943f)
                // V 2.07342z
                verticalLineTo(y = 2.07342f)
                close()
            }
            // M11.25 9.04057 L3 11.7906 V11.9914 C3 17.6294 7.23896 20.3655 9.89856 21.5273 C10.4093 21.7504 10.7392 21.8945 11.25 21.9597 V9.04057Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 11.25 9.04057
                moveTo(x = 11.25f, y = 9.04057f)
                // L 3 11.7906
                lineTo(x = 3.0f, y = 11.7906f)
                // V 11.9914
                verticalLineTo(y = 11.9914f)
                // C 3 17.6294 7.23896 20.3655 9.89856 21.5273
                curveTo(
                    x1 = 3.0f,
                    y1 = 17.6294f,
                    x2 = 7.23896f,
                    y2 = 20.3655f,
                    x3 = 9.89856f,
                    y3 = 21.5273f,
                )
                // C 10.4093 21.7504 10.7392 21.8945 11.25 21.9597
                curveTo(
                    x1 = 10.4093f,
                    y1 = 21.7504f,
                    x2 = 10.7392f,
                    y2 = 21.8945f,
                    x3 = 11.25f,
                    y3 = 21.9597f,
                )
                // V 9.04057z
                verticalLineTo(y = 9.04057f)
                close()
            }
            // M12.75 21.9597 V9.04057 L21 11.7906 V11.9914 C21 17.6294 16.761 20.3655 14.1014 21.5273 C13.5907 21.7504 13.2608 21.8945 12.75 21.9597Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12.75 21.9597
                moveTo(x = 12.75f, y = 21.9597f)
                // V 9.04057
                verticalLineTo(y = 9.04057f)
                // L 21 11.7906
                lineTo(x = 21.0f, y = 11.7906f)
                // V 11.9914
                verticalLineTo(y = 11.9914f)
                // C 21 17.6294 16.761 20.3655 14.1014 21.5273
                curveTo(
                    x1 = 21.0f,
                    y1 = 17.6294f,
                    x2 = 16.761f,
                    y2 = 20.3655f,
                    x3 = 14.1014f,
                    y3 = 21.5273f,
                )
                // C 13.5907 21.7504 13.2608 21.8945 12.75 21.9597z
                curveTo(
                    x1 = 13.5907f,
                    y1 = 21.7504f,
                    x2 = 13.2608f,
                    y2 = 21.8945f,
                    x3 = 12.75f,
                    y3 = 21.9597f,
                )
                close()
            }
            // M12.75 7.45943 V2.07342 C13.3563 2.18652 14.0684 2.43028 15.1623 2.80472 L15.7351 3.00079 C18.7417 4.02996 20.245 4.54454 20.6225 5.08241 C20.9918 5.60853 20.9998 7.14974 21 10.2094 L12.75 7.45943Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12.75 7.45943
                moveTo(x = 12.75f, y = 7.45943f)
                // V 2.07342
                verticalLineTo(y = 2.07342f)
                // C 13.3563 2.18652 14.0684 2.43028 15.1623 2.80472
                curveTo(
                    x1 = 13.3563f,
                    y1 = 2.18652f,
                    x2 = 14.0684f,
                    y2 = 2.43028f,
                    x3 = 15.1623f,
                    y3 = 2.80472f,
                )
                // L 15.7351 3.00079
                lineTo(x = 15.7351f, y = 3.00079f)
                // C 18.7417 4.02996 20.245 4.54454 20.6225 5.08241
                curveTo(
                    x1 = 18.7417f,
                    y1 = 4.02996f,
                    x2 = 20.245f,
                    y2 = 4.54454f,
                    x3 = 20.6225f,
                    y3 = 5.08241f,
                )
                // C 20.9918 5.60853 20.9998 7.14974 21 10.2094
                curveTo(
                    x1 = 20.9918f,
                    y1 = 5.60853f,
                    x2 = 20.9998f,
                    y2 = 7.14974f,
                    x3 = 21.0f,
                    y3 = 10.2094f,
                )
                // L 12.75 7.45943z
                lineTo(x = 12.75f, y = 7.45943f)
                close()
            }
        }.build().also { _solarPrivacy = it }
    }


@Suppress("ObjectPropertyName")
private var _solarPrivacy: ImageVector? = null
