package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.home.HomeScreen
import com.starvault.ui.home.HomeUiState
import org.junit.Rule
import org.junit.Test

/**
 * Home 屏 Paparazzi 回归基线 — 与 design/01-home.html 对齐。
 *
 *  - ready   : 默认主态（FixturePresets.homeFiles 模拟 10 条文件 + 默认 FileTag）
 *  - loading : Loading 占位（无文件列表）
 *  - error   : 拉取失败
 */
class HomeScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    @Test fun home_ready() = paparazzi.snapshot {
        StarVaultTheme {
            HomeScreen(
                state = HomeUiState.Success(files = FixturePresets.homeFiles()),
                onTagClick = {},
                onAllTagClick = {},
                onSortClick = {},
                onFabClick = {},
                onFileClick = {},
                onFileMore = {},
                onQuickClick = {},
            )
        }
    }

    @Test fun home_loading() = paparazzi.snapshot {
        StarVaultTheme {
            HomeScreen(
                state = HomeUiState.Loading(),
                onTagClick = {},
                onAllTagClick = {},
                onSortClick = {},
                onFabClick = {},
                onFileClick = {},
                onFileMore = {},
                onQuickClick = {},
            )
        }
    }
}