package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FileImageFill: ImageVector
    get() {
        val current = _fileImageFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FileImageFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M158.66 219.56 A8 8 0 0 1 152 232 H24 a8 8 0 0 1 -6.73 -12.33 l36 -56 a8 8 0 0 1 13.46 0 l9.76 15.18 20.85 -31.29 a8 8 0 0 1 13.32 0Z M216 88 V216 a16 16 0 0 1 -16 16 h-8 a8 8 0 0 1 0 -16 h8 V96 H152 a8 8 0 0 1 -8 -8 V40 H56 v88 a8 8 0 0 1 -16 0 V40 A16 16 0 0 1 56 24 h96 a8 8 0 0 1 5.66 2.34 l56 56 A8 8 0 0 1 216 88Z m-56 -8 h28.69 L160 51.31Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 158.66 219.56
                moveTo(x = 158.66f, y = 219.56f)
                // A 8 8 0 0 1 152 232
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 152.0f,
                    y1 = 232.0f,
                )
                // H 24
                horizontalLineTo(x = 24.0f)
                // a 8 8 0 0 1 -6.73 -12.33
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -6.73f,
                    dy1 = -12.33f,
                )
                // l 36 -56
                lineToRelative(dx = 36.0f, dy = -56.0f)
                // a 8 8 0 0 1 13.46 0
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 13.46f,
                    dy1 = 0.0f,
                )
                // l 9.76 15.18
                lineToRelative(dx = 9.76f, dy = 15.18f)
                // l 20.85 -31.29
                lineToRelative(dx = 20.85f, dy = -31.29f)
                // a 8 8 0 0 1 13.32 0z
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 13.32f,
                    dy1 = 0.0f,
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
                // h -8
                horizontalLineToRelative(dx = -8.0f)
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
                // h 8
                horizontalLineToRelative(dx = 8.0f)
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
                // v 88
                verticalLineToRelative(dy = 88.0f)
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
        }.build().also { _fileImageFill = it }
    }

@Suppress("ObjectPropertyName")
private var _fileImageFill: ImageVector? = null
