package com.starvault.data.local.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * 媒体文件播放进度持久化（Video + Audio 共用，进程内 DataStore Preferences）。
 *
 *  设计目标：
 *  - 进程内 rememberSaveable 不够 —— app 被系统杀 / 主动滑掉重进会丢位置
 *  - 用 DataStore Preferences 把 (fid → positionMs) 存盘，下次进 PreviewVideo / PreviewAudio 屏时
 *    VM 在 emit Success 之前调 [load]，让 Screen 在 player ready 时立刻 seekTo
 *  - 卸载 / 跨设备恢复超出本 M5 scope（需 115 `/open/ufile/position` 服务端记录，
 *    跟 OpenList / p115client 调研一致 — 当前 v1 走本地落盘，UI 引导用户预期是"本机记忆"）
 *
 *  适用范围：
 *  - [com.starvault.ui.preview.PreviewVideoViewModel]：进屏读 saved position,5s 节流 + onDispose 兜底写
 *  - [com.starvault.ui.preview.PreviewAudioViewModel]：同上(原本走 [AudioPositionStore],M5 重命名为共用 store)
 *
 *  Key 格式：每条 entry = 一个 `longPreferencesKey("media_pos_$fid")`
 *  - 单文件粒度（不分组类型），video / audio 同 key namespace 不冲突(file id 全局唯一)
 *  - 写入由 Preview{Video,Audio}Screen 节流 5s 一次 + onDispose 兜底
 *  - 读在 VM.load() 第一次 emit Success 前完成
 *
 *  DataStore name = `media_positions`，跟 OpenAuthStore 的 `cloud115_tokens` 分文件，
 *  避免一个屏清空 token 时连带清空播放进度。
 *
 *  改名记录（M5 中段）：
 *  - 此前为 AudioPositionStore(name `audio_positions`,key `audio_pos_$fid`),AudioOnly
 *  - 现 MediaPositionStore(name `media_positions`,key `media_pos_$fid`),video + audio 共用
 *  - 旧 DataStore 文件保留在磁盘但不读;清空用 adb shell pm clear 即可
 */
private val Context.mediaPositionsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "media_positions")

class MediaPositionStore(private val context: Context) {

    private val store: DataStore<Preferences> = context.mediaPositionsDataStore

    /**
     * 读取指定 fid 上次保存的播放位置（毫秒）。
     *
     * @param fid 115 文件 id（PreviewVideo / PreviewAudio Route.fileId）
     * @return 上次的 positionMs；未保存过 / 数据损坏 → null（UI 端视为"从头开始"）
     */
    suspend fun load(fid: String): Long? {
        val prefs = store.data.first()
        return prefs[longPreferencesKey(keyFor(fid))]
    }

    /**
     * 保存播放位置（毫秒）。
     *
     * 由 Screen 端 5s 节流 + onDispose 兜底调用，VM 不直接调（VM 不持有 ExoPlayer 实例）。
     *
     * @param fid 115 文件 id
     * @param positionMs 当前 playhead 位置（毫秒）;<0 视为非法,直接 return
     */
    suspend fun save(fid: String, positionMs: Long) {
        if (positionMs < 0) return
        store.edit { prefs ->
            prefs[longPreferencesKey(keyFor(fid))] = positionMs
        }
    }

    /** 构造稳定的 preferences key,每个 fid 对应一个独立 entry。 */
    private fun keyFor(fid: String): String = "media_pos_$fid"
}