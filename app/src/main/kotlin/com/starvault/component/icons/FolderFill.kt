package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FolderFill: ImageVector
    get() {
        val current = _folderFill
        if (current != null) return current

        return ImageVector.Builder(
            name = "com.starvault.theme.StarVaultTheme.FolderFill",
            defaultWidth = 256.0.dp,
            defaultHeight = 256.0.dp,
            viewportWidth = 256.0f,
            viewportHeight = 256.0f,
        ).apply {
            // M216 72 H131.31 L104 44.69 A15.88 15.88 0 0 0 92.69 40 H40 A16 16 0 0 0 24 56 V200.62 A15.41 15.41 0 0 0 39.39 216 h177.5 A15.13 15.13 0 0 0 232 200.89 V88 A16 16 0 0 0 216 72Z M40 56 H92.69 l16 16 H40Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 216 72
                moveTo(x = 216.0f, y = 72.0f)
                // H 131.31
                horizontalLineTo(x = 131.31f)
                // L 104 44.69
                lineTo(x = 104.0f, y = 44.69f)
                // A 15.88 15.88 0 0 0 92.69 40
                arcTo(
                    horizontalEllipseRadius = 15.88f,
                    verticalEllipseRadius = 15.88f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 92.69f,
                    y1 = 40.0f,
                )
                // H 40
                horizontalLineTo(x = 40.0f)
                // A 16 16 0 0 0 24 56
                arcTo(
                    horizontalEllipseRadius = 16.0f,
                    verticalEllipseRadius = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 24.0f,
                    y1 = 56.0f,
                )
                // V 200.62
                verticalLineTo(y = 200.62f)
                // A 15.41 15.41 0 0 0 39.39 216
                arcTo(
                    horizontalEllipseRadius = 15.41f,
                    verticalEllipseRadius = 15.41f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 39.39f,
                    y1 = 216.0f,
                )
                // h 177.5
                horizontalLineToRelative(dx = 177.5f)
                // A 15.13 15.13 0 0 0 232 200.89
                arcTo(
                    horizontalEllipseRadius = 15.13f,
                    verticalEllipseRadius = 15.13f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 232.0f,
                    y1 = 200.89f,
                )
                // V 88
                verticalLineTo(y = 88.0f)
                // A 16 16 0 0 0 216 72z
                arcTo(
                    horizontalEllipseRadius = 16.0f,
                    verticalEllipseRadius = 16.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 216.0f,
                    y1 = 72.0f,
                )
                close()
                // M 40 56
                moveTo(x = 40.0f, y = 56.0f)
                // H 92.69
                horizontalLineTo(x = 92.69f)
                // l 16 16
                lineToRelative(dx = 16.0f, dy = 16.0f)
                // H 40z
                horizontalLineTo(x = 40.0f)
                close()
            }
        }.build().also { _folderFill = it }
    }

@Suppress("ObjectPropertyName")
private var _folderFill: ImageVector? = null
