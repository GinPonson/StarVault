/*
 * Solar Bold "Bell" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Notifications/Bell.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarBell: ImageVector
    get() {
        val current = _solarBell
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarBell",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M8.35179 20.2418 C9.19288 21.311 10.5142 22 12 22 C13.4858 22 14.8071 21.311 15.6482 20.2418 C13.2264 20.57 10.7736 20.57 8.35179 20.2418Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 8.35179 20.2418
                moveTo(x = 8.35179f, y = 20.2418f)
                // C 9.19288 21.311 10.5142 22 12 22
                curveTo(
                    x1 = 9.19288f,
                    y1 = 21.311f,
                    x2 = 10.5142f,
                    y2 = 22.0f,
                    x3 = 12.0f,
                    y3 = 22.0f,
                )
                // C 13.4858 22 14.8071 21.311 15.6482 20.2418
                curveTo(
                    x1 = 13.4858f,
                    y1 = 22.0f,
                    x2 = 14.8071f,
                    y2 = 21.311f,
                    x3 = 15.6482f,
                    y3 = 20.2418f,
                )
                // C 13.2264 20.57 10.7736 20.57 8.35179 20.2418z
                curveTo(
                    x1 = 13.2264f,
                    y1 = 20.57f,
                    x2 = 10.7736f,
                    y2 = 20.57f,
                    x3 = 8.35179f,
                    y3 = 20.2418f,
                )
                close()
            }
            // M18.7491 9 V9.7041 C18.7491 10.5491 18.9903 11.3752 19.4422 12.0782 L20.5496 13.8012 C21.5612 15.3749 20.789 17.5139 19.0296 18.0116 C14.4273 19.3134 9.57274 19.3134 4.97036 18.0116 C3.21105 17.5139 2.43882 15.3749 3.45036 13.8012 L4.5578 12.0782 C5.00972 11.3752 5.25087 10.5491 5.25087 9.7041 V9 C5.25087 5.13401 8.27256 2 12 2 C15.7274 2 18.7491 5.13401 18.7491 9Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 18.7491 9
                moveTo(x = 18.7491f, y = 9.0f)
                // V 9.7041
                verticalLineTo(y = 9.7041f)
                // C 18.7491 10.5491 18.9903 11.3752 19.4422 12.0782
                curveTo(
                    x1 = 18.7491f,
                    y1 = 10.5491f,
                    x2 = 18.9903f,
                    y2 = 11.3752f,
                    x3 = 19.4422f,
                    y3 = 12.0782f,
                )
                // L 20.5496 13.8012
                lineTo(x = 20.5496f, y = 13.8012f)
                // C 21.5612 15.3749 20.789 17.5139 19.0296 18.0116
                curveTo(
                    x1 = 21.5612f,
                    y1 = 15.3749f,
                    x2 = 20.789f,
                    y2 = 17.5139f,
                    x3 = 19.0296f,
                    y3 = 18.0116f,
                )
                // C 14.4273 19.3134 9.57274 19.3134 4.97036 18.0116
                curveTo(
                    x1 = 14.4273f,
                    y1 = 19.3134f,
                    x2 = 9.57274f,
                    y2 = 19.3134f,
                    x3 = 4.97036f,
                    y3 = 18.0116f,
                )
                // C 3.21105 17.5139 2.43882 15.3749 3.45036 13.8012
                curveTo(
                    x1 = 3.21105f,
                    y1 = 17.5139f,
                    x2 = 2.43882f,
                    y2 = 15.3749f,
                    x3 = 3.45036f,
                    y3 = 13.8012f,
                )
                // L 4.5578 12.0782
                lineTo(x = 4.5578f, y = 12.0782f)
                // C 5.00972 11.3752 5.25087 10.5491 5.25087 9.7041
                curveTo(
                    x1 = 5.00972f,
                    y1 = 11.3752f,
                    x2 = 5.25087f,
                    y2 = 10.5491f,
                    x3 = 5.25087f,
                    y3 = 9.7041f,
                )
                // V 9
                verticalLineTo(y = 9.0f)
                // C 5.25087 5.13401 8.27256 2 12 2
                curveTo(
                    x1 = 5.25087f,
                    y1 = 5.13401f,
                    x2 = 8.27256f,
                    y2 = 2.0f,
                    x3 = 12.0f,
                    y3 = 2.0f,
                )
                // C 15.7274 2 18.7491 5.13401 18.7491 9z
                curveTo(
                    x1 = 15.7274f,
                    y1 = 2.0f,
                    x2 = 18.7491f,
                    y2 = 5.13401f,
                    x3 = 18.7491f,
                    y3 = 9.0f,
                )
                close()
            }
        }.build().also { _solarBell = it }
    }


@Suppress("ObjectPropertyName")
private var _solarBell: ImageVector? = null
