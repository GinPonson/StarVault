package com.starvault.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 圆角 token。命名沿用 mockup CSS 类（xs / sm / md / lg / xl / xxl / pill）。
 *
 *  - xs (3dp)  ：tag chip / 小标识
 *  - sm (4dp)  ：input
 *  - md (9dp)  ：按钮 / 卡片（小）
 *  - lg (12dp) ：大卡片
 *  - xl (13dp) ：大弹窗
 *  - xxl(28dp) ：底部 sheet 顶角
 *  - pill      ：胶囊（搜索框、tab 选中态）
 */
@Immutable
data class StarVaultShapes(
    val xs:   Shape = RoundedCornerShape(3.dp),
    val sm:   Shape = RoundedCornerShape(4.dp),
    val md:   Shape = RoundedCornerShape(9.dp),
    val lg:   Shape = RoundedCornerShape(12.dp),
    val xl:   Shape = RoundedCornerShape(13.dp),
    val xxl:  Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(999.dp),
)
