package com.starvault.fixtures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FixturePresets 烟雾测试：每个 preset 至少 1 条 / 关键字段非空。
 * 任何 preset 写空都会立即被这个测试抓住。
 */
class FixturePresetsTest {

    @Test fun `home files are non-empty`() = assertTrue(FixturePresets.homeFiles().isNotEmpty())

    @Test fun `transfers are non-empty`() {
        val t = FixturePresets.transfers()
        assertTrue(t.isNotEmpty())
        assertTrue(t.all { it.totalBytes > 0 })
    }

    @Test fun `profile has valid capacity`() {
        val u = FixturePresets.profile()
        assertTrue(u.usedBytes in 0..u.totalBytes)
        assertEquals("Vint", u.nickname)
    }

    @Test fun `share links filter by fileId`() {
        val links = FixturePresets.shareLinksFor("f02")
        assertTrue(links.isNotEmpty())
        assertTrue(links.all { it.fileId == "f02" })
    }

    @Test fun `album photos have positive dimensions`() {
        assertTrue(FixturePresets.albumPhotos().all { it.width > 0 && it.height > 0 })
    }

    @Test fun `wallpapers and config are aligned`() {
        val ws = FixturePresets.wallpapers()
        val cfg = FixturePresets.wallpaperConfig()
        assertTrue(ws.isNotEmpty())
        assertTrue(cfg.categories.isNotEmpty())
    }
}
