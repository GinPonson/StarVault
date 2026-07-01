package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FileZipFill: ImageVector
    get() {
        val current = _fileZipFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FileZipFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M184 144 H168 a8 8 0 0 0 -8 8 v55.73 a8.17 8.17 0 0 0 7.47 8.25 8 8 0 0 0 8.53 -8 v-8 h7.4 c15.24 0 28.14 -11.92 28.59 -27.15 A28 28 0 0 0 184 144Z m-.35 40 H176 V160 h8 A12 12 0 0 1 196 173.16 12.25 12.25 0 0 1 183.65 184Z M136 152 v55.73 a8.17 8.17 0 0 1 -7.47 8.25 8 8 0 0 1 -8.53 -8 V152.27 a8.17 8.17 0 0 1 7.47 -8.25 A8 8 0 0 1 136 152Z M96 208.53 A8.17 8.17 0 0 1 87.73 216 H56.23 a8.27 8.27 0 0 1 -6 -2.5 A8 8 0 0 1 49.05 204 l25.16 -44 H56.27 A8.17 8.17 0 0 1 48 152.53 8 8 0 0 1 56 144 H87.77 a8.27 8.27 0 0 1 6 2.5 A8 8 0 0 1 95 156 L69.79 200 H88 A8 8 0 0 1 96 208.53Z M213.66 82.34 l-56 -56 A8 8 0 0 0 152 24 H56 A16 16 0 0 0 40 40 v76 a4 4 0 0 0 4 4 H212 a4 4 0 0 0 4 -4 V88 A8 8 0 0 0 213.66 82.34Z M152 88 V44 l44 44Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 184 144
                moveTo(x = 184.0f, y = 144.0f)
                // H 168
                horizontalLineTo(x = 168.0f)
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
                // v 55.73
                verticalLineToRelative(dy = 55.73f)
                // a 8.17 8.17 0 0 0 7.47 8.25
                arcToRelative(
                    a = 8.17f,
                    b = 8.17f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 7.47f,
                    dy1 = 8.25f,
                )
                // a 8 8 0 0 0 8.53 -8
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 8.53f,
                    dy1 = -8.0f,
                )
                // v -8
                verticalLineToRelative(dy = -8.0f)
                // h 7.4
                horizontalLineToRelative(dx = 7.4f)
                // c 15.24 0 28.14 -11.92 28.59 -27.15
                curveToRelative(
                    dx1 = 15.24f,
                    dy1 = 0.0f,
                    dx2 = 28.14f,
                    dy2 = -11.92f,
                    dx3 = 28.59f,
                    dy3 = -27.15f,
                )
                // A 28 28 0 0 0 184 144z
                arcTo(
                    horizontalEllipseRadius = 28.0f,
                    verticalEllipseRadius = 28.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 184.0f,
                    y1 = 144.0f,
                )
                close()
                // m -0.35 40
                moveToRelative(dx = -0.35f, dy = 40.0f)
                // H 176
                horizontalLineTo(x = 176.0f)
                // V 160
                verticalLineTo(y = 160.0f)
                // h 8
                horizontalLineToRelative(dx = 8.0f)
                // A 12 12 0 0 1 196 173.16
                arcTo(
                    horizontalEllipseRadius = 12.0f,
                    verticalEllipseRadius = 12.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 196.0f,
                    y1 = 173.16f,
                )
                // A 12.25 12.25 0 0 1 183.65 184z
                arcTo(
                    horizontalEllipseRadius = 12.25f,
                    verticalEllipseRadius = 12.25f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 183.65f,
                    y1 = 184.0f,
                )
                close()
                // M 136 152
                moveTo(x = 136.0f, y = 152.0f)
                // v 55.73
                verticalLineToRelative(dy = 55.73f)
                // a 8.17 8.17 0 0 1 -7.47 8.25
                arcToRelative(
                    a = 8.17f,
                    b = 8.17f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7.47f,
                    dy1 = 8.25f,
                )
                // a 8 8 0 0 1 -8.53 -8
                arcToRelative(
                    a = 8.0f,
                    b = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -8.53f,
                    dy1 = -8.0f,
                )
                // V 152.27
                verticalLineTo(y = 152.27f)
                // a 8.17 8.17 0 0 1 7.47 -8.25
                arcToRelative(
                    a = 8.17f,
                    b = 8.17f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 7.47f,
                    dy1 = -8.25f,
                )
                // A 8 8 0 0 1 136 152z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 136.0f,
                    y1 = 152.0f,
                )
                close()
                // M 96 208.53
                moveTo(x = 96.0f, y = 208.53f)
                // A 8.17 8.17 0 0 1 87.73 216
                arcTo(
                    horizontalEllipseRadius = 8.17f,
                    verticalEllipseRadius = 8.17f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 87.73f,
                    y1 = 216.0f,
                )
                // H 56.23
                horizontalLineTo(x = 56.23f)
                // a 8.27 8.27 0 0 1 -6 -2.5
                arcToRelative(
                    a = 8.27f,
                    b = 8.27f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -6.0f,
                    dy1 = -2.5f,
                )
                // A 8 8 0 0 1 49.05 204
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 49.05f,
                    y1 = 204.0f,
                )
                // l 25.16 -44
                lineToRelative(dx = 25.16f, dy = -44.0f)
                // H 56.27
                horizontalLineTo(x = 56.27f)
                // A 8.17 8.17 0 0 1 48 152.53
                arcTo(
                    horizontalEllipseRadius = 8.17f,
                    verticalEllipseRadius = 8.17f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 48.0f,
                    y1 = 152.53f,
                )
                // A 8 8 0 0 1 56 144
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 56.0f,
                    y1 = 144.0f,
                )
                // H 87.77
                horizontalLineTo(x = 87.77f)
                // a 8.27 8.27 0 0 1 6 2.5
                arcToRelative(
                    a = 8.27f,
                    b = 8.27f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.0f,
                    dy1 = 2.5f,
                )
                // A 8 8 0 0 1 95 156
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 95.0f,
                    y1 = 156.0f,
                )
                // L 69.79 200
                lineTo(x = 69.79f, y = 200.0f)
                // H 88
                horizontalLineTo(x = 88.0f)
                // A 8 8 0 0 1 96 208.53z
                arcTo(
                    horizontalEllipseRadius = 8.0f,
                    verticalEllipseRadius = 8.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 96.0f,
                    y1 = 208.53f,
                )
                close()
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
                // v 76
                verticalLineToRelative(dy = 76.0f)
                // a 4 4 0 0 0 4 4
                arcToRelative(
                    a = 4.0f,
                    b = 4.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 4.0f,
                    dy1 = 4.0f,
                )
                // H 212
                horizontalLineTo(x = 212.0f)
                // a 4 4 0 0 0 4 -4
                arcToRelative(
                    a = 4.0f,
                    b = 4.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 4.0f,
                    dy1 = -4.0f,
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
                // M 152 88
                moveTo(x = 152.0f, y = 88.0f)
                // V 44
                verticalLineTo(y = 44.0f)
                // l 44 44z
                lineToRelative(dx = 44.0f, dy = 44.0f)
                close()
            }
        }.build().also { _fileZipFill = it }
    }

@Suppress("ObjectPropertyName")
private var _fileZipFill: ImageVector? = null
