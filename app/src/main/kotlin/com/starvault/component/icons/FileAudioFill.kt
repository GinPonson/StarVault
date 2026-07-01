package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FileAudioFill: ImageVector
    get() {
        val current = _fileAudioFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FileAudioFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M152 180 a40.55 40.55 0 0 1 -20 34.91 A8 8 0 0 1 124 201.09 a24.49 24.49 0 0 0 0 -42.18 A8 8 0 0 1 132 145.09 40.55 40.55 0 0 1 152 180Z M99.06 128.61 a8 8 0 0 0 -8.72 1.73 L68.69 152 H48 a8 8 0 0 0 -8 8 v40 a8 8 0 0 0 8 8 H68.69 l21.65 21.66 A8 8 0 0 0 104 224 V136 A8 8 0 0 0 99.06 128.61Z M216 88 V216 a16 16 0 0 1 -16 16 H168 a8 8 0 0 1 0 -16 h32 V96 H152 a8 8 0 0 1 -8 -8 V40 H56 v80 a8 8 0 0 1 -16 0 V40 A16 16 0 0 1 56 24 h96 a8 8 0 0 1 5.66 2.34 l56 56 A8 8 0 0 1 216 88Z m-56 -8 h28.69 L160 51.31Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 152 180
                moveTo(x = 152.0f, y = 180.0f)
                // a 40.55 40.55 0 0 1 -20 34.91
                arcToRelative(
                    a = 40.55f,
                    b = 40.55f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -20.0f,
                    dy1 = 34.91f,
                )
                // A 8 8 0 0 1 124 201.09
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 124.0f,
                    y1 = 201.09f,
                )
                // a 24.49 24.49 0 0 0 0 -42.18
                arcToRelative(
                    a = 24.49f,
                    b = 24.49f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -42.18f,
                )
                // A 8 8 0 0 1 132 145.09
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 132.0f,
                    y1 = 145.09f,
                )
                // A 40.55 40.55 0 0 1 152 180z
                arcTo(
                    horizontalEllipseRadius = 40.55f,
                    verticalEllipseRadius = 40.55f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 152.0f,
                    y1 = 180.0f,
                )
                close()
                // M 99.06 128.61
                moveTo(x = 99.06f, y = 128.61f)
                // a 8 8 0 0 0 -8.72 1.73
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -8.72f,
                    dy1 = 1.73f,
                )
                // L 68.69 152
                lineTo(x = 68.69f, y = 152.0f)
                // H 48
                horizontalLineTo(x = 48.0f)
                // a 8 8 0 0 0 -8 8
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -8.0f,
                    dy1 = 8.0f,
                )
                // v 40
                verticalLineToRelative(dy = 40.0f)
                // a 8 8 0 0 0 8 8
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 8.0f,
                    dy1 = 8.0f,
                )
                // H 68.69
                horizontalLineTo(x = 68.69f)
                // l 21.65 21.66
                lineToRelative(dx = 21.65f, dy = 21.66f)
                // A 8 8 0 0 0 104 224
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 104.0f,
                    y1 = 224.0f,
                )
                // V 136
                verticalLineTo(y = 136.0f)
                // A 8 8 0 0 0 99.06 128.61z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 99.06f,
                    y1 = 128.61f,
                )
                close()
                // M 216 88
                moveTo(x = 216.0f, y = 88.0f)
                // V 216
                verticalLineTo(y = 216.0f)
                // a 16 16 0 0 1 -16 16
                arcToRelative(
                    a = 16.0f,
                    b = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -16.0f,
                    dy1 = 16.0f,
                )
                // H 168
                horizontalLineTo(x = 168.0f)
                // a 8 8 0 0 1 0 -16
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.0f,
                    dy1 = -16.0f,
                )
                // h 32
                horizontalLineToRelative(dx = 32.0f)
                // V 96
                verticalLineTo(y = 96.0f)
                // H 152
                horizontalLineTo(x = 152.0f)
                // a 8 8 0 0 1 -8 -8
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -8.0f,
                    dy1 = -8.0f,
                )
                // V 40
                verticalLineTo(y = 40.0f)
                // H 56
                horizontalLineTo(x = 56.0f)
                // v 80
                verticalLineToRelative(dy = 80.0f)
                // a 8 8 0 0 1 -16 0
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -16.0f,
                    dy1 = 0.0f,
                )
                // V 40
                verticalLineTo(y = 40.0f)
                // A 16 16 0 0 1 56 24
                arcTo(
                    horizontalEllipseRadius = 16.0f,
                    verticalEllipseRadius = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 56.0f,
                    y1 = 24.0f,
                )
                // h 96
                horizontalLineToRelative(dx = 96.0f)
                // a 8 8 0 0 1 5.66 2.34
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 5.66f,
                    dy1 = 2.34f,
                )
                // l 56 56
                lineToRelative(dx = 56.0f, dy = 56.0f)
                // A 8 8 0 0 1 216 88z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 216.0f,
                    y1 = 88.0f,
                )
                close()
                // m -56 -8
                moveToRelative(dx = -56.0f, dy = -8.0f)
                // h 28.69
                horizontalLineToRelative(dx = 28.69f)
                // L 160 51.31z
                lineTo(x = 160.0f, y = 51.31f)
                close()
            }
        }.build().also { _fileAudioFill = it }
    }

@Suppress("ObjectPropertyName")
private var _fileAudioFill: ImageVector? = null
