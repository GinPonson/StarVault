package com.starvault.ui.login

/**
 * Login 屏 UiState（与 design/00-login.html 的扫码状态机对齐）。
 *
 * 状态流（mock）：
 *   Waiting ─── simulateScan() ──▶ Scanned ─── (1.8s) ──▶ LoggedIn
 *       ▲                                                   │
 *       └────── refresh() ←──────── 任意状态 / 过期自动 ────┘
 *
 *  - Waiting   : "等待扫码…"，dot pulse 动画
 *  - Scanned   : 已扫码，QR 上盖 overlay（头像 + 用户名 + 设备）
 *  - LoggedIn  : "登录成功，正在跳转…"，由 Route 触发 onLoggedIn 跳 Home
 *  - Error     : 仅在网络/会话异常时出现（mock 阶段暂未触发，但接口保留）
 */
sealed interface LoginUiState {
    /** 二维码过期倒计时（秒）。Phase 1 用 fixture 120s。*/
    val expireSeconds: Int

    /** 等待用户扫码。*/
    data class Waiting(override val expireSeconds: Int = 120) : LoginUiState

    /** 已扫码：手机端弹出确认页，等用户点击「确认登录」。*/
    data class Scanned(
        val nickname: String,
        val deviceName: String,
        override val expireSeconds: Int = 120,
    ) : LoginUiState

    /** 登录成功。Route 监听到此状态后调用 `onLoggedIn` 触发导航。*/
    data class LoggedIn(
        val nickname: String,
        override val expireSeconds: Int = 120,
    ) : LoginUiState

    /** 异常（如二维码服务不可达）。*/
    data class Error(
        val message: String,
        override val expireSeconds: Int = 0,
    ) : LoginUiState
}
