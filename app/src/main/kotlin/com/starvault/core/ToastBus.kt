package com.starvault.core

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 全局 Toast 事件。
 *
 * ViewModel 端 [ToastBus.error] / [ToastBus.info] send,
 * Composable 端 [ToastHost] 收集 [ToastBus.events] 转 Snackbar。
 */
sealed interface ToastEvent {
    /**
     * @param text    展示文案(必填,空串会被 Snackbar 截断)
     * @param isError 是否错误提示(true 默认;error 通道与 info 通道未来可分样式)
     */
    data class Message(val text: String, val isError: Boolean = true) : ToastEvent
}

/**
 * 进程级单例的 Toast 事件总线。
 *
 *  - 用 `object` 而非 [ServiceLocator]:无 Context 依赖、无 init 契约,
 *    挂进 ServiceLocator 反而污染其 `@Volatile lateinit + private set` 强约束。
 *  - [Channel.UNLIMITED] + trySend:与 ProfileViewModel 原 Effect 同 queue 语义,
 *    subscriber 启动后从 queue 头取,不会像 SharedFlow(replay = 0)那样丢值。
 *  - [Log.w] 兜底:即使没有 Composable 订阅,设备也能从 logcat 看到错误。
 *  - 单测:用 `mockkObject(ToastBus)` + `every { ToastBus.error(any()) } returns Unit` stub。
 */
object ToastBus {
    private const val TAG = "ToastBus"
    private val _events = Channel<ToastEvent>(Channel.UNLIMITED)
    val events: Flow<ToastEvent> = _events.receiveAsFlow()

    /** 发送错误提示(默认红色 Snackbar 文案)。 */
    fun error(message: String) {
        Log.w(TAG, message)
        _events.trySend(ToastEvent.Message(message, isError = true))
    }

    /** 发送信息提示(占位,目前等价于 error 样式)。 */
    fun info(message: String) {
        _events.trySend(ToastEvent.Message(message, isError = false))
    }
}
