package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FileFill: ImageVector
    get() {
        val current = _fileFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FileFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M213.66 82.34 l-56 -56 A8 8 0 0 0 152 24 H56 A16 16 0 0 0 40 40 V216 a16 16 0 0 0 16 16 H200 a16 16 0 0 0 16 -16 V88 A8 8 0 0 0 213.66 82.34Z M152 88 V44 l44 44Z
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
                // V 216
                verticalLineTo(y = 216.0f)
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
                // H 200
                horizontalLineTo(x = 200.0f)
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
                // M 152 88
                moveTo(x = 152.0f, y = 88.0f)
                // V 44
                verticalLineTo(y = 44.0f)
                // l 44 44z
                lineToRelative(dx = 44.0f, dy = 44.0f)
                close()
            }
        }.build().also { _fileFill = it }
    }

@Suppress("ObjectPropertyName")
private var _fileFill: ImageVector? = null
