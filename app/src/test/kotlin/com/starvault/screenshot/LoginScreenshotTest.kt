package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.login.LoginScreen
import com.starvault.ui.login.LoginUiState
import org.junit.Rule
import org.junit.Test

/**
 * Login 屏 Paparazzi 4-state 回归基线 — 与 design/00-login.html 状态机对齐。
 *
 *  - waiting  : 等待扫码（dot pulse 动画，截图取 t=0 静态帧）
 *  - scanned  : 已扫码，QR 上盖 overlay（mock 何湘湘 + MacBook Pro 14）
 *  - loggedIn : 登录成功（dot fill + "正在跳转…"）
 *  - error    : 二维码服务不可达（mock "扫码超时"）
 */
class LoginScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    @Test fun login_waiting() = paparazzi.snapshot {
        StarVaultTheme {
            LoginScreen(
                state = LoginUiState.Waiting(expireSeconds = 120),
                onScanClick = {},
                onRefresh = {},
            )
        }
    }

    @Test fun login_scanned() = paparazzi.snapshot {
        StarVaultTheme {
            LoginScreen(
                state = LoginUiState.Scanned(
                    nickname = "何湘湘",
                    deviceName = "MacBook Pro 14",
                    expireSeconds = 87,
                ),
                onScanClick = {},
                onRefresh = {},
            )
        }
    }

    @Test fun login_loggedIn() = paparazzi.snapshot {
        StarVaultTheme {
            LoginScreen(
                state = LoginUiState.LoggedIn(nickname = "何湘湘"),
                onScanClick = {},
                onRefresh = {},
            )
        }
    }

    @Test fun login_error() = paparazzi.snapshot {
        StarVaultTheme {
            LoginScreen(
                state = LoginUiState.Error(message = "扫码超时，请刷新二维码"),
                onScanClick = {},
                onRefresh = {},
            )
        }
    }
}