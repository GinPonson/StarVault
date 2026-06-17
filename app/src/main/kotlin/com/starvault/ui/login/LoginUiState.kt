package com.starvault.ui.login

import androidx.compose.ui.graphics.ImageBitmap

sealed interface LoginUiState {
    val expireSeconds: Int

    data class Waiting(
        val qrBitmap: ImageBitmap? = null,
        override val expireSeconds: Int = 120,
    ) : LoginUiState

    data class Scanned(
        val nickname: String,
        val deviceName: String,
        override val expireSeconds: Int = 120,
    ) : LoginUiState

    data class LoggedIn(
        val nickname: String,
        override val expireSeconds: Int = 0,
    ) : LoginUiState

    data class Error(
        val message: String,
        override val expireSeconds: Int = 0,
    ) : LoginUiState
}
