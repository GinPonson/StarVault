package com.starvault.ui.profile

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.component.Icons
import com.starvault.core.ServiceLocator
import com.starvault.data.repository.AuthRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Profile 屏 ViewModel — Phase 1 mock + 115 signOut 接入。
 *
 *  - storage / wallpaper / rows 仍是 mock（等 webapi 接入再做真数据）
 *  - [onSignOut] 真接 [AuthRepository.signOut] → DataStore 清 cookies →
 *    authState 切 Unauthenticated → NavHost 自动跳回 Login（无需手动 nav.popUp）
 *
 *  signOut 错误（极少见 — DataStore.clear 在磁盘满时可能抛 IOException）
 *  通过 [effect] 发 [Effect.Error]，让 Route 端用 Snackbar 展示。
 */
class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
) : ViewModel() {

    companion object { private const val TAG = "ProfileViewModel" }

    private val _state = MutableStateFlow<ProfileUiState>(mockState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /** 一次性事件（Snackbar / Toast）。
     *  用 Channel(UNLIMITED) 而非 SharedFlow：UNLIMITED capacity + trySend 永不挂起；
     *  且 Channel 是 queue 语义，subscriber 启动后从 queue 头取，不会像 SharedFlow replay=0 那样丢值。 */
    private val _effect = Channel<Effect>(capacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    /** 退出登录：调 [AuthRepository.signOut] 清 cookies。NavHost 监听到 authState 切 Unauthenticated 会自动跳 Login。
     *
     *  实现为 suspend → 由 Route 端用 rememberCoroutineScope().launch 触发，UI 不阻塞。
     */
    suspend fun onSignOut() {
        try {
            authRepository.signOut()
            // 成功：什么都不做，NavHost 自动跳
        } catch (e: Throwable) {
            // catch Throwable 而非 Exception：android.util.Log 在 JVM unit test 下会抛
            // UnsatisfiedLinkError（Error 子类），绕过 Exception catch 导致测试 fail
            // 这里用 println 兜底：生产靠 logcat，测试靠 stdout
            println("[$TAG] signOut failed: ${e.message}")
            _effect.trySend(Effect.Error(e.message ?: "退出登录失败"))
        }
    }

    /** 一次性 UI 事件。 */
    sealed interface Effect {
        data class Error(val message: String) : Effect
    }

    /* ─────────────────── mock state ─────────────────── */

    private fun mockState(): ProfileUiState.Success {
        val storage = Storage(
            usedPct = 71,
            totalLabel = "1 TB",
            releaseDate = "2026/06/07 释放",
            breakdowns = listOf(
                Breakdown("视频",  Color(0xFF2F6FEB), "112.4 GB"),
                Breakdown("图片",  Color(0xFF9333EA), "48.2 GB"),
                Breakdown("文档",  Color(0xFF16A34A), "12.7 GB"),
                Breakdown("音频",  Color(0xFFEA580C), "38.5 GB"),
                Breakdown("其他",  Color(0xFFD4D4D4), "26.6 GB"),
            ),
            remainingGb = "761.6 GB",
            trashGb = "2.1 GB",
        )
        val wallpaper = Wallpaper(
            enabled = false,
            subText = "让相册成为会动的壁纸",
        )
        val commonRows = listOf(
            RowItem(Icons.ShareOut,  iconAccent = false, label = "我的分享", rightText = "12 个进行中"),
            RowItem(Icons.Trash,     iconAccent = false, label = "回收站",   rightText = "2.1 GB"),
            RowItem(Icons.Device,    iconAccent = false, label = "设备管理", rightText = "3 台"),
        )
        val settingRows = listOf(
            RowItem(Icons.Privacy,    iconAccent = false, label = "隐私与安全"),
            RowItem(Icons.Appearance, iconAccent = false, label = "外观与主题", rightText = "跟随系统"),
            RowItem(Icons.Help,       iconAccent = false, label = "帮助与反馈", rightBadge = "v6.2.1"),
        )
        return ProfileUiState.Success(
            storage = storage,
            wallpaper = wallpaper,
            commonRows = commonRows,
            settingRows = settingRows,
        )
    }
}
