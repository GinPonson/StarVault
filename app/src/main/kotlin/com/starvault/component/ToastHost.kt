package com.starvault.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.starvault.core.ToastBus
import com.starvault.core.ToastEvent
import com.starvault.theme.StarVaultTheme

/**
 * 全局 Toast 渲染器(订阅 [ToastBus.events],把每个事件转 Snackbar)。
 *
 *  挂载点:`StarVaultApp` 的 `Scaffold(snackbarHost = { ... })`,
 *  跟 Scaffold 默认 snackbarHost slot 集成(不挂 MainActivity 外层 Box 避免双 host 冲突)。
 *
 *  padding bottom = bottomBarHeight:避免与 BottomNavBar 重叠
 *  (ProfileRoute 之前的 hardcode 80dp 同理,这里统一抽成 token)。
 */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        ToastBus.events.collect { event ->
            when (event) {
                is ToastEvent.Message -> snackbarHostState.showSnackbar(
                    message = event.text,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(bottom = StarVaultTheme.dimens.bottomBarHeight),
    )
}
