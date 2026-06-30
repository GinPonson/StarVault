package com.starvault.ui.files

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FAB scroll-aware 方向判定纯函数单测。
 *
 * 核心约束:`resolveFabScrollDir` 必须是纯函数(无副作用、无 coroutine),
 * 否则 Compose LaunchedEffect 测试要拉 Robolectric / Compose UI Test,得不偿失。
 *
 * 阈值:默认 [FAB_SCROLL_THRESHOLD_PX] = 4px(对齐竞品手感 — 小幅抖动不触发 FAB 切换)。
 *
 * 行为表:
 * ```
 *   delta >  threshold → DOWN  (FabScrollDir.DOWN)
 *   delta < -threshold → UP    (FabScrollDir.UP)
 *   |delta| ≤ threshold → IDLE (FabScrollDir.IDLE)
 * ```
 */
class FilesScreenFabScrollTest {

    @Test
    fun `delta greater than threshold returns DOWN`() {
        // 边界:delta = threshold + 1 → 严格大于,判定 DOWN
        assertEquals(FabScrollDir.DOWN, resolveFabScrollDir(delta = 5))
        assertEquals(FabScrollDir.DOWN, resolveFabScrollDir(delta = 100))
        assertEquals(FabScrollDir.DOWN, resolveFabScrollDir(delta = 999_999))
    }

    @Test
    fun `delta less than negative threshold returns UP`() {
        assertEquals(FabScrollDir.UP, resolveFabScrollDir(delta = -5))
        assertEquals(FabScrollDir.UP, resolveFabScrollDir(delta = -100))
        assertEquals(FabScrollDir.UP, resolveFabScrollDir(delta = -999_999))
    }

    @Test
    fun `delta within threshold band returns IDLE including zero`() {
        // 边界 ±threshold 包含在内 → IDLE(防止噪声抖动触发 FAB 闪屏)
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = 0))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = 1))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = 4))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = -1))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = -4))
    }

    @Test
    fun `exact threshold boundary returns IDLE (not DOWN or UP)`() {
        // 边界严格性:`>` 和 `<` 而不是 `>=` 和 `<=`
        // delta = threshold  → IDLE(差值不够越过阈值)
        // delta = -threshold → IDLE
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = FAB_SCROLL_THRESHOLD_PX))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta = -FAB_SCROLL_THRESHOLD_PX))
    }

    @Test
    fun `threshold parameter overrides default`() {
        // 用更大阈值时,同样的 delta 应该从 DOWN 变 IDLE — 验证 threshold 是参数而非硬编码
        val delta = 50
        assertEquals(FabScrollDir.DOWN, resolveFabScrollDir(delta, threshold = 4))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(delta, threshold = 60))
        assertEquals(FabScrollDir.DOWN, resolveFabScrollDir(delta, threshold = 10))

        val negDelta = -50
        assertEquals(FabScrollDir.UP, resolveFabScrollDir(negDelta, threshold = 4))
        assertEquals(FabScrollDir.IDLE, resolveFabScrollDir(negDelta, threshold = 60))
    }
}