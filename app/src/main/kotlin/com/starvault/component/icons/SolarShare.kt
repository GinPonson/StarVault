/*
 * Solar Bold "Share" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Essentional,%20UI/Share.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarShare: ImageVector
    get() {
        val current = _solarShare
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarShare",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M13.803 5.33333 C13.803 3.49238 15.3022 2 17.1515 2 C19.0008 2 20.5 3.49238 20.5 5.33333 C20.5 7.17428 19.0008 8.66667 17.1515 8.66667 C16.2177 8.66667 15.3738 8.28596 14.7671 7.67347 L10.1317 10.8295 C10.1745 11.0425 10.197 11.2625 10.197 11.4872 C10.197 11.9322 10.109 12.3576 9.94959 12.7464 L15.0323 16.0858 C15.6092 15.6161 16.3473 15.3333 17.1515 15.3333 C19.0008 15.3333 20.5 16.8257 20.5 18.6667 C20.5 20.5076 19.0008 22 17.1515 22 C15.3022 22 13.803 20.5076 13.803 18.6667 C13.803 18.1845 13.9062 17.7255 14.0917 17.3111 L9.05007 13.9987 C8.46196 14.5098 7.6916 14.8205 6.84848 14.8205 C4.99917 14.8205 3.5 13.3281 3.5 11.4872 C3.5 9.64623 4.99917 8.15385 6.84848 8.15385 C7.9119 8.15385 8.85853 8.64725 9.47145 9.41518 L13.9639 6.35642 C13.8594 6.03359 13.803 5.6896 13.803 5.33333Z
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 13.803 5.33333
                moveTo(x = 13.803f, y = 5.33333f)
                // C 13.803 3.49238 15.3022 2 17.1515 2
                curveTo(
                    x1 = 13.803f,
                    y1 = 3.49238f,
                    x2 = 15.3022f,
                    y2 = 2.0f,
                    x3 = 17.1515f,
                    y3 = 2.0f,
                )
                // C 19.0008 2 20.5 3.49238 20.5 5.33333
                curveTo(
                    x1 = 19.0008f,
                    y1 = 2.0f,
                    x2 = 20.5f,
                    y2 = 3.49238f,
                    x3 = 20.5f,
                    y3 = 5.33333f,
                )
                // C 20.5 7.17428 19.0008 8.66667 17.1515 8.66667
                curveTo(
                    x1 = 20.5f,
                    y1 = 7.17428f,
                    x2 = 19.0008f,
                    y2 = 8.66667f,
                    x3 = 17.1515f,
                    y3 = 8.66667f,
                )
                // C 16.2177 8.66667 15.3738 8.28596 14.7671 7.67347
                curveTo(
                    x1 = 16.2177f,
                    y1 = 8.66667f,
                    x2 = 15.3738f,
                    y2 = 8.28596f,
                    x3 = 14.7671f,
                    y3 = 7.67347f,
                )
                // L 10.1317 10.8295
                lineTo(x = 10.1317f, y = 10.8295f)
                // C 10.1745 11.0425 10.197 11.2625 10.197 11.4872
                curveTo(
                    x1 = 10.1745f,
                    y1 = 11.0425f,
                    x2 = 10.197f,
                    y2 = 11.2625f,
                    x3 = 10.197f,
                    y3 = 11.4872f,
                )
                // C 10.197 11.9322 10.109 12.3576 9.94959 12.7464
                curveTo(
                    x1 = 10.197f,
                    y1 = 11.9322f,
                    x2 = 10.109f,
                    y2 = 12.3576f,
                    x3 = 9.94959f,
                    y3 = 12.7464f,
                )
                // L 15.0323 16.0858
                lineTo(x = 15.0323f, y = 16.0858f)
                // C 15.6092 15.6161 16.3473 15.3333 17.1515 15.3333
                curveTo(
                    x1 = 15.6092f,
                    y1 = 15.6161f,
                    x2 = 16.3473f,
                    y2 = 15.3333f,
                    x3 = 17.1515f,
                    y3 = 15.3333f,
                )
                // C 19.0008 15.3333 20.5 16.8257 20.5 18.6667
                curveTo(
                    x1 = 19.0008f,
                    y1 = 15.3333f,
                    x2 = 20.5f,
                    y2 = 16.8257f,
                    x3 = 20.5f,
                    y3 = 18.6667f,
                )
                // C 20.5 20.5076 19.0008 22 17.1515 22
                curveTo(
                    x1 = 20.5f,
                    y1 = 20.5076f,
                    x2 = 19.0008f,
                    y2 = 22.0f,
                    x3 = 17.1515f,
                    y3 = 22.0f,
                )
                // C 15.3022 22 13.803 20.5076 13.803 18.6667
                curveTo(
                    x1 = 15.3022f,
                    y1 = 22.0f,
                    x2 = 13.803f,
                    y2 = 20.5076f,
                    x3 = 13.803f,
                    y3 = 18.6667f,
                )
                // C 13.803 18.1845 13.9062 17.7255 14.0917 17.3111
                curveTo(
                    x1 = 13.803f,
                    y1 = 18.1845f,
                    x2 = 13.9062f,
                    y2 = 17.7255f,
                    x3 = 14.0917f,
                    y3 = 17.3111f,
                )
                // L 9.05007 13.9987
                lineTo(x = 9.05007f, y = 13.9987f)
                // C 8.46196 14.5098 7.6916 14.8205 6.84848 14.8205
                curveTo(
                    x1 = 8.46196f,
                    y1 = 14.5098f,
                    x2 = 7.6916f,
                    y2 = 14.8205f,
                    x3 = 6.84848f,
                    y3 = 14.8205f,
                )
                // C 4.99917 14.8205 3.5 13.3281 3.5 11.4872
                curveTo(
                    x1 = 4.99917f,
                    y1 = 14.8205f,
                    x2 = 3.5f,
                    y2 = 13.3281f,
                    x3 = 3.5f,
                    y3 = 11.4872f,
                )
                // C 3.5 9.64623 4.99917 8.15385 6.84848 8.15385
                curveTo(
                    x1 = 3.5f,
                    y1 = 9.64623f,
                    x2 = 4.99917f,
                    y2 = 8.15385f,
                    x3 = 6.84848f,
                    y3 = 8.15385f,
                )
                // C 7.9119 8.15385 8.85853 8.64725 9.47145 9.41518
                curveTo(
                    x1 = 7.9119f,
                    y1 = 8.15385f,
                    x2 = 8.85853f,
                    y2 = 8.64725f,
                    x3 = 9.47145f,
                    y3 = 9.41518f,
                )
                // L 13.9639 6.35642
                lineTo(x = 13.9639f, y = 6.35642f)
                // C 13.8594 6.03359 13.803 5.6896 13.803 5.33333z
                curveTo(
                    x1 = 13.8594f,
                    y1 = 6.03359f,
                    x2 = 13.803f,
                    y2 = 5.6896f,
                    x3 = 13.803f,
                    y3 = 5.33333f,
                )
                close()
            }
        }.build().also { _solarShare = it }
    }


@Suppress("ObjectPropertyName")
private var _solarShare: ImageVector? = null
