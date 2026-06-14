package com.starvault.component

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// 统一 Icon 源。stub 是空 ImageVector 占位（Valkyrie 上线后用真实 SVG 替换）；
// 部分需要立即可识别的 icon 用 unicode 字符（Glyph）实现，避免空 path 在 Paparazzi 渲染成空白。
object Icons {
    val Home      = stub("Home")
    val Files     = stub("Files")
    val Album     = stub("Album")
    val Profile   = stub("Profile")
    val More      = stub("More")
    val Back      = stub("Back")
    val Close     = stub("Close")
    val Play      = stub("Play")
    val Pause     = stub("Pause")
    val Search    = stub("Search")
    val Star      = stub("Star")
    val Scan      = stub("Scan")
    val Bell      = stub("Bell")
}

/** 空 ImageVector 占位（Valkyrie 上线后用真实 SVG 替换）。 */
private fun stub(name: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).build()
