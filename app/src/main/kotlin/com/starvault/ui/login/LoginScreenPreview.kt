package com.starvault.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * 4 个 Preview，覆盖 LoginScreen 的 4 个 UiState。
 * Paparazzi T22 会针对这些 Preview 做截图回归。
 *
 * widthDp/heightDp 与 design HTML 的 device frame 一致 (412 x 900)。
 */
@Preview(name = "Login/Waiting",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun LoginWaitingPreview() = StarVaultTheme {
    LoginScreen(
        state = LoginUiState.Waiting(expireSeconds = 120),
        onScanClick = {},
        onRefresh = {},
    )
}

@Preview(name = "Login/Scanned",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun LoginScannedPreview() = StarVaultTheme {
    LoginScreen(
        state = LoginUiState.Scanned(
            nickname = "Gin",
            deviceName = "iPhone 15 Pro · 来自广东",
            expireSeconds = 118,
        ),
        onScanClick = {},
        onRefresh = {},
    )
}

@Preview(name = "Login/LoggedIn", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun LoginLoggedInPreview() = StarVaultTheme {
    LoginScreen(
        state = LoginUiState.LoggedIn(nickname = "Gin", expireSeconds = 116),
        onScanClick = {},
        onRefresh = {},
    )
}

@Preview(name = "Login/Error",    showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun LoginErrorPreview() = StarVaultTheme {
    LoginScreen(
        state = LoginUiState.Error("二维码服务不可达"),
        onScanClick = {},
        onRefresh = {},
    )
}
