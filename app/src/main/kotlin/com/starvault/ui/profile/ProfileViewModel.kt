package com.starvault.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.repository.AuthRepository
import com.starvault.data.repository.UserInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Profile 屏 ViewModel — mock + proapi /open/user/info 接入。
 *
 *  - storage.usedPct / totalLabel / remainingGb 来自 proapi /open/user/info 的 rt_space_info（真）
 *  - storage.vipLevelName 来自 vip_info.level_name（真，"年费VIP"/"超级VIP"…）
 *  - storage.userName 来自 user_name（真）→ StorageCard 标题展示 + 触发 VIP 徽章
 *  - storage.trashGb 走 mock（OAuth Open 平台未开放 rb 字段）
 *  - storage.breakdowns 永远走 mock（type_summury 8 类不在 Open 平台开放范围）
 *
 *  - [onSignOut] 真接 [AuthRepository.signOut] → DataStore 清 cookies →
 *    authState 切 Unauthenticated → NavHost 自动跳回 Login（无需手动 nav.popUp）
 *
 *  signOut / fetchUserInfo 错误（极少见 — DataStore.clear 抛 IOException / proapi 失败）
 *  通过 [effect] 发 [Effect.Error],让 Route 端用 Snackbar 展示。
 */
class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(mockState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /** 一次性事件（Snackbar / Toast）。
     *  用 Channel(UNLIMITED) 而非 SharedFlow：UNLIMITED capacity + trySend 永不挂起；
     *  且 Channel 是 queue 语义，subscriber 启动后从 queue 头取，不会像 SharedFlow replay=0 那样丢值。 */
    private val _effect = Channel<Effect>(capacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    init {
        loadUserInfo()
    }

    /** 从 proapi /open/user/info 拉 user info + rt_space_info + vip_info，merge 进 mock state。失败 fallback mock + Snackbar。 */
    private fun loadUserInfo() {
        viewModelScope.launch {
            authRepository.fetchUserInfo()
                .onSuccess { userInfo -> applyUserInfo(userInfo) }
                .onFailure { e ->
                    println("[ProfileViewModel] fetchUserInfo failed: ${e.message}")
                    _effect.trySend(Effect.Error(e.message ?: "用户信息加载失败"))
                }
        }
    }

    /**
     * 把 [UserInfo] 合并进 [ProfileUiState.Success]。
     *
     *  - usedPct / totalLabel / remainingGb 从 [com.starvault.data.remote.cloud115.RtSpaceInfo] 计算
     *    (实测 115 返回 size 是 Long 字节数,size_format 是 115 服务端格式化好的串)
     *  - vipLevelName 直接透传;空字符串 → UI 不显示徽章
     *  - trashGb / breakdowns 保持 mock(Open 平台未开放)
     */
    private fun applyUserInfo(userInfo: UserInfo) {
        val current = _state.value as? ProfileUiState.Success ?: return
        val space = userInfo.space
        if (space != null && space.allTotal.size > 0L) {
            // 优先用 115 服务端格式化好的串(如 "49.49TB"),无 fallback 才本地 formatBytes
            val totalBytes = space.allTotal.size
            val usedBytes = space.allUse.size
            val remainBytes = space.allRemain.size
            // usedPct:all_use / all_total × 100(整数百分比,UI 直接用)
            val usedPct = if (totalBytes > 0L) ((usedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0
            _state.value = current.copy(
                storage = current.storage.copy(
                    userName = userInfo.base.userName.orEmpty(),
                    usedPct = usedPct,
                    totalLabel = space.allTotal.sizeFormat.ifBlank { formatBytes(totalBytes) },
                    remainingGb = space.allRemain.sizeFormat.ifBlank { formatBytes(remainBytes) },
                    vipLevelName = userInfo.vip?.levelName.orEmpty(),
                    // breakdowns / trashGb 保持原 mock(Open 平台未开放)
                ),
            )
        } else {
            // 无空间数据(可能新用户/服务端偶发):只更新 userName + vipLevelName
            _state.value = current.copy(
                storage = current.storage.copy(
                    userName = userInfo.base.userName.orEmpty(),
                    vipLevelName = userInfo.vip?.levelName.orEmpty(),
                ),
            )
        }
    }

    /** 字节数 → "1.2 GB" / "512.0 MB" 风格（1 位小数）。fallback 用,115 正常会返回 size_format。 */
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024L
        val mb = kb * 1024
        val gb = mb * 1024
        val tb = gb * 1024
        return when {
            bytes >= tb -> "%.1f TB".format(bytes.toDouble() / tb)
            bytes >= gb -> "%.1f GB".format(bytes.toDouble() / gb)
            bytes >= mb -> "%.1f MB".format(bytes.toDouble() / mb)
            bytes >= kb -> "%.1f KB".format(bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }

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
            println("[ProfileViewModel] signOut failed: ${e.message}")
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
            breakdowns = listOf(
                Breakdown("视频",  androidx.compose.ui.graphics.Color(0xFF2F6FEB), "112.4 GB"),
                Breakdown("图片",  androidx.compose.ui.graphics.Color(0xFF9333EA), "48.2 GB"),
                Breakdown("文档",  androidx.compose.ui.graphics.Color(0xFF16A34A), "12.7 GB"),
                Breakdown("音频",  androidx.compose.ui.graphics.Color(0xFFEA580C), "38.5 GB"),
                Breakdown("其他",  androidx.compose.ui.graphics.Color(0xFFD4D4D4), "26.6 GB"),
            ),
            remainingGb = "761.6 GB",
            trashGb = "2.1 GB",
            userName = "",
            vipLevelName = "",
            breakdownsIsMock = true,
        )
        val wallpaper = Wallpaper(
            enabled = false,
            subText = "让相册成为会动的壁纸",
        )
        val commonRows = listOf(
            RowItem(com.starvault.component.Icons.ShareOut,  iconAccent = false, label = "我的分享", rightText = "12 个进行中"),
            RowItem(com.starvault.component.Icons.Trash,     iconAccent = false, label = "回收站",   rightText = "2.1 GB"),
            RowItem(com.starvault.component.Icons.Device,    iconAccent = false, label = "设备管理", rightText = "3 台"),
        )
        val settingRows = listOf(
            RowItem(com.starvault.component.Icons.Privacy,    iconAccent = false, label = "隐私与安全"),
            RowItem(com.starvault.component.Icons.Appearance, iconAccent = false, label = "外观与主题", rightText = "跟随系统"),
            RowItem(com.starvault.component.Icons.Help,       iconAccent = false, label = "帮助与反馈", rightBadge = "v6.2.1"),
        )
        return ProfileUiState.Success(
            storage = storage,
            wallpaper = wallpaper,
            commonRows = commonRows,
            settingRows = settingRows,
        )
    }
}
