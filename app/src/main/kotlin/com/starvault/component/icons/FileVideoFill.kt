package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FileVideoFill: ImageVector
    get() {
        val current = _fileVideoFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FileVideoFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M213.66 82.34 l-56 -56 A8 8 0 0 0 152 24 H56 A16 16 0 0 0 40 40 v72 a8 8 0 0 0 16 0 V40 h88 V88 a8 8 0 0 0 8 8 h48 V216 h-8 a8 8 0 0 0 0 16 h8 a16 16 0 0 0 16 -16 V88 A8 8 0 0 0 213.66 82.34Z M160 51.31 188.69 80 H160Z M155.88 145 a8 8 0 0 0 -8.12 .22 l-19.95 12.46 A16 16 0 0 0 112 144 H48 a16 16 0 0 0 -16 16 v48 a16 16 0 0 0 16 16 h64 a16 16 0 0 0 15.81 -13.68 l19.95 12.46 A8 8 0 0 0 160 216 V152 A8 8 0 0 0 155.88 145Z M144 201.57 l-16 -10 V176.43 l16 -10Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 213.66 82.34
                moveTo(x = 213.66f, y = 82.34f)
                // l -56 -56
                lineToRelative(dx = -56.0f, dy = -56.0f)
                // A 8 8 0 0 0 152 24
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 152.0f,
                    y1 = 24.0f,
                )
                // H 56
                horizontalLineTo(x = 56.0f)
                // A 16 16 0 0 0 40 40
                arcTo(
                    horizontalEllipseRadius = 16.0f,
                    verticalEllipseRadius = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 40.0f,
                    y1 = 40.0f,
                )
                // v 72
                verticalLineToRelative(dy = 72.0f)
                // a 8 8 0 0 0 16 0
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 16.0f,
                    dy1 = 0.0f,
                )
                // V 40
                verticalLineTo(y = 40.0f)
                // h 88
                horizontalLineToRelative(dx = 88.0f)
                // V 88
                verticalLineTo(y = 88.0f)
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
                // h 48
                horizontalLineToRelative(dx = 48.0f)
                // V 216
                verticalLineTo(y = 216.0f)
                // h -8
                horizontalLineToRelative(dx = -8.0f)
                // a 8 8 0 0 0 0 16
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 16.0f,
                )
                // h 8
                horizontalLineToRelative(dx = 8.0f)
                // a 16 16 0 0 0 16 -16
                arcToRelative(
                    a = 16.0f,
                    b = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 16.0f,
                    dy1 = -16.0f,
                )
                // V 88
                verticalLineTo(y = 88.0f)
                // A 8 8 0 0 0 213.66 82.34z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 213.66f,
                    y1 = 82.34f,
                )
                close()
                // M 160 51.31
                moveTo(x = 160.0f, y = 51.31f)
                // L 188.69 80
                lineTo(x = 188.69f, y = 80.0f)
                // H 160z
                horizontalLineTo(x = 160.0f)
                close()
                // M 155.88 145
                moveTo(x = 155.88f, y = 145.0f)
                // a 8 8 0 0 0 -8.12 0.22
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -8.12f,
                    dy1 = 0.22f,
                )
                // l -19.95 12.46
                lineToRelative(dx = -19.95f, dy = 12.46f)
                // A 16 16 0 0 0 112 144
                arcTo(
                    horizontalEllipseRadius = 16.0f,
                    verticalEllipseRadius = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 112.0f,
                    y1 = 144.0f,
                )
                // H 48
                horizontalLineTo(x = 48.0f)
                // a 16 16 0 0 0 -16 16
                arcToRelative(
                    a = 16.0f,
                    b = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -16.0f,
                    dy1 = 16.0f,
                )
                // v 48
                verticalLineToRelative(dy = 48.0f)
                // a 16 16 0 0 0 16 16
                arcToRelative(
                    a = 16.0f,
                    b = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 16.0f,
                    dy1 = 16.0f,
                )
                // h 64
                horizontalLineToRelative(dx = 64.0f)
                // a 16 16 0 0 0 15.81 -13.68
                arcToRelative(
                    a = 16.0f,
                    b = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 15.81f,
                    dy1 = -13.68f,
                )
                // l 19.95 12.46
                lineToRelative(dx = 19.95f, dy = 12.46f)
                // A 8 8 0 0 0 160 216
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 160.0f,
                    y1 = 216.0f,
                )
                // V 152
                verticalLineTo(y = 152.0f)
                // A 8 8 0 0 0 155.88 145z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 155.88f,
                    y1 = 145.0f,
                )
                close()
                // M 144 201.57
                moveTo(x = 144.0f, y = 201.57f)
                // l -16 -10
                lineToRelative(dx = -16.0f, dy = -10.0f)
                // V 176.43
                verticalLineTo(y = 176.43f)
                // l 16 -10z
                lineToRelative(dx = 16.0f, dy = -10.0f)
                close()
            }
        }.build().also { _fileVideoFill = it }
    }

@Suppress("ObjectPropertyName")
private var _fileVideoFill: ImageVector? = null
