package com.starvault.component

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// 统一 stub Icon 源（Valkyrie 插件未启用前先用空 ImageVector 占位）。
//
// 重要：T22-T30 的 Paparazzi 截图会渲染成空 icon slot；
// 用 Valkyrie 插件从 design 目录下的 mockup SVG 批量转出后，
// 只替换 stub(name) 内的 path data 即可，不动其它调用点。
object Icons {
    val Home      = stub("Home")
    val Files     = stub("Files")
    val Transfers = stub("Transfers")
    val Profile   = stub("Profile")
    val More      = stub("More")
    val Back      = stub("Back")
    val Close     = stub("Close")
    val Play      = stub("Play")
    val Pause     = stub("Pause")
    val Search    = stub("Search")
    val Star      = stub("Star")
}

private fun stub(name: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).build()
