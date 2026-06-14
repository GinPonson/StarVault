package com.starvault.data

import android.content.Context
import com.starvault.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.InputStream

// FixtureLoader: 读 assets/fixtures/*.json → kotlinx.serialization 反序列化
//
// - loadFromAssets: 同步入口（运行时从 Context.assets 读）
// - loadFromStream: 同步入口（单测直接传 ByteArrayInputStream，绕开 final AssetManager）
// - loadDelayed:    异步入口（debug build 默认延迟 300ms 模拟网络）
object FixtureLoader {
    // @PublishedApi：让 inline reified 函数能引用
    @PublishedApi internal val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T> loadFromAssets(context: Context, assetPath: String): T {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString<T>(text)
    }

    inline fun <reified T> loadFromStream(stream: InputStream): T {
        val text = stream.bufferedReader().use { it.readText() }
        return json.decodeFromString<T>(text)
    }

    suspend inline fun <reified T> loadDelayed(
        context: Context,
        assetPath: String,
        delayMs: Long = if (BuildConfig.MOCK_DELAY) 300L else 0L,
    ): T {
        if (delayMs > 0) delay(delayMs)
        return loadFromAssets(context, assetPath)
    }
}
