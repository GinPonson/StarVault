// 根项目：只声明插件版本（apply false），实际应用到 :app 模块在 T3 的 build.gradle.kts
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.compose.compiler)     apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
