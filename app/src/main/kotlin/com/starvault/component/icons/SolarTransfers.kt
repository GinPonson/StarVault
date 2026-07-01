/*
 * Solar Bold "Sort Vertical" icon from 480-Design/Solar-Icon-Set (CC BY 4.0).
 * https://github.com/480-Design/Solar-Icon-Set/blob/main/icons/SVG/Bold/Arrows/Sort%20Vertical.svg
 */
package com.starvault.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SolarTransfers: ImageVector
    get() {
        val current = _solarTransfers
        if (current != null) return current

        return ImageVector.Builder(
            name = "StarVault.SolarTransfers",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 13.125 C12.3013 13.125 12.5733 13.3052 12.6907 13.5827 C12.8081 13.8601 12.7482 14.1808 12.5384 14.3971 L8.53844 18.5221 C8.39719 18.6678 8.20293 18.75 8.00002 18.75 C7.79711 18.75 7.60285 18.6678 7.46159 18.5221 L3.46159 14.3971 C3.25188 14.1808 3.19192 13.8601 3.30934 13.5827 C3.42676 13.3052 3.69877 13.125 4.00002 13.125 H7.25002 V6 C7.25002 5.58579 7.5858 5.25 8.00002 5.25 C8.41423 5.25 8.75002 5.58579 8.75002 6 V13.125 H12Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 12 13.125
                moveTo(x = 12.0f, y = 13.125f)
                // C 12.3013 13.125 12.5733 13.3052 12.6907 13.5827
                curveTo(
                    x1 = 12.3013f,
                    y1 = 13.125f,
                    x2 = 12.5733f,
                    y2 = 13.3052f,
                    x3 = 12.6907f,
                    y3 = 13.5827f,
                )
                // C 12.8081 13.8601 12.7482 14.1808 12.5384 14.3971
                curveTo(
                    x1 = 12.8081f,
                    y1 = 13.8601f,
                    x2 = 12.7482f,
                    y2 = 14.1808f,
                    x3 = 12.5384f,
                    y3 = 14.3971f,
                )
                // L 8.53844 18.5221
                lineTo(x = 8.53844f, y = 18.5221f)
                // C 8.39719 18.6678 8.20293 18.75 8.00002 18.75
                curveTo(
                    x1 = 8.39719f,
                    y1 = 18.6678f,
                    x2 = 8.20293f,
                    y2 = 18.75f,
                    x3 = 8.00002f,
                    y3 = 18.75f,
                )
                // C 7.79711 18.75 7.60285 18.6678 7.46159 18.5221
                curveTo(
                    x1 = 7.79711f,
                    y1 = 18.75f,
                    x2 = 7.60285f,
                    y2 = 18.6678f,
                    x3 = 7.46159f,
                    y3 = 18.5221f,
                )
                // L 3.46159 14.3971
                lineTo(x = 3.46159f, y = 14.3971f)
                // C 3.25188 14.1808 3.19192 13.8601 3.30934 13.5827
                curveTo(
                    x1 = 3.25188f,
                    y1 = 14.1808f,
                    x2 = 3.19192f,
                    y2 = 13.8601f,
                    x3 = 3.30934f,
                    y3 = 13.5827f,
                )
                // C 3.42676 13.3052 3.69877 13.125 4.00002 13.125
                curveTo(
                    x1 = 3.42676f,
                    y1 = 13.3052f,
                    x2 = 3.69877f,
                    y2 = 13.125f,
                    x3 = 4.00002f,
                    y3 = 13.125f,
                )
                // H 7.25002
                horizontalLineTo(x = 7.25002f)
                // V 6
                verticalLineTo(y = 6.0f)
                // C 7.25002 5.58579 7.5858 5.25 8.00002 5.25
                curveTo(
                    x1 = 7.25002f,
                    y1 = 5.58579f,
                    x2 = 7.5858f,
                    y2 = 5.25f,
                    x3 = 8.00002f,
                    y3 = 5.25f,
                )
                // C 8.41423 5.25 8.75002 5.58579 8.75002 6
                curveTo(
                    x1 = 8.41423f,
                    y1 = 5.25f,
                    x2 = 8.75002f,
                    y2 = 5.58579f,
                    x3 = 8.75002f,
                    y3 = 6.0f,
                )
                // V 13.125
                verticalLineTo(y = 13.125f)
                // H 12z
                horizontalLineTo(x = 12.0f)
                close()
            }
            // M20 10.875 C20.3013 10.875 20.5733 10.6948 20.6907 10.4173 C20.8081 10.1399 20.7482 9.81916 20.5384 9.60289 L16.5384 5.47789 C16.3972 5.33222 16.2029 5.25 16 5.25 C15.7971 5.25 15.6029 5.33222 15.4616 5.47789 L11.4616 9.60289 C11.2519 9.81916 11.1919 10.1399 11.3093 10.4173 C11.4268 10.6948 11.6988 10.875 12 10.875 H15.25 V18 C15.25 18.4142 15.5858 18.75 16 18.75 C16.4142 18.75 16.75 18.4142 16.75 18 L16.75 10.875 H20Z
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 20 10.875
                moveTo(x = 20.0f, y = 10.875f)
                // C 20.3013 10.875 20.5733 10.6948 20.6907 10.4173
                curveTo(
                    x1 = 20.3013f,
                    y1 = 10.875f,
                    x2 = 20.5733f,
                    y2 = 10.6948f,
                    x3 = 20.6907f,
                    y3 = 10.4173f,
                )
                // C 20.8081 10.1399 20.7482 9.81916 20.5384 9.60289
                curveTo(
                    x1 = 20.8081f,
                    y1 = 10.1399f,
                    x2 = 20.7482f,
                    y2 = 9.81916f,
                    x3 = 20.5384f,
                    y3 = 9.60289f,
                )
                // L 16.5384 5.47789
                lineTo(x = 16.5384f, y = 5.47789f)
                // C 16.3972 5.33222 16.2029 5.25 16 5.25
                curveTo(
                    x1 = 16.3972f,
                    y1 = 5.33222f,
                    x2 = 16.2029f,
                    y2 = 5.25f,
                    x3 = 16.0f,
                    y3 = 5.25f,
                )
                // C 15.7971 5.25 15.6029 5.33222 15.4616 5.47789
                curveTo(
                    x1 = 15.7971f,
                    y1 = 5.25f,
                    x2 = 15.6029f,
                    y2 = 5.33222f,
                    x3 = 15.4616f,
                    y3 = 5.47789f,
                )
                // L 11.4616 9.60289
                lineTo(x = 11.4616f, y = 9.60289f)
                // C 11.2519 9.81916 11.1919 10.1399 11.3093 10.4173
                curveTo(
                    x1 = 11.2519f,
                    y1 = 9.81916f,
                    x2 = 11.1919f,
                    y2 = 10.1399f,
                    x3 = 11.3093f,
                    y3 = 10.4173f,
                )
                // C 11.4268 10.6948 11.6988 10.875 12 10.875
                curveTo(
                    x1 = 11.4268f,
                    y1 = 10.6948f,
                    x2 = 11.6988f,
                    y2 = 10.875f,
                    x3 = 12.0f,
                    y3 = 10.875f,
                )
                // H 15.25
                horizontalLineTo(x = 15.25f)
                // V 18
                verticalLineTo(y = 18.0f)
                // C 15.25 18.4142 15.5858 18.75 16 18.75
                curveTo(
                    x1 = 15.25f,
                    y1 = 18.4142f,
                    x2 = 15.5858f,
                    y2 = 18.75f,
                    x3 = 16.0f,
                    y3 = 18.75f,
                )
                // C 16.4142 18.75 16.75 18.4142 16.75 18
                curveTo(
                    x1 = 16.4142f,
                    y1 = 18.75f,
                    x2 = 16.75f,
                    y2 = 18.4142f,
                    x3 = 16.75f,
                    y3 = 18.0f,
                )
                // L 16.75 10.875
                lineTo(x = 16.75f, y = 10.875f)
                // H 20z
                horizontalLineTo(x = 20.0f)
                close()
            }
        }.build().also { _solarTransfers = it }
    }


@Suppress("ObjectPropertyName")
private var _solarTransfers: ImageVector? = null
