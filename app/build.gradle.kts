plugins {
    // AGP 9.0+ 内置 Kotlin 支持，不再需要 org.jetbrains.kotlin.android
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.starvault"
    // activity-compose 1.13.0 要求 compileSdk ≥ 36（plan 写 35 不兼容，已修正）
    compileSdk = 36

    defaultConfig {
        applicationId = "com.starvault"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("Boolean", "MOCK_DELAY", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "MOCK_DELAY", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
        getByName("test").kotlin.srcDirs("src/test/kotlin")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    // 编译/测试都用 JDK 21（Gradle 自动从 Adoptium 下载，无需本机配置）。
    // 字节码目标仍是 JVM_17（兼容 minSdk 34 / 现有设备）。
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.nav.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.ser)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // QR 编码（OpenAuthManager 把 115 返回的 qrcodeUrl 字符串渲染成 Bitmap 给 LoginScreen）
    implementation(libs.zxing.core)
    // Media3 视频播放（PreviewVideo 用，播放 115 m3u8 流）
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)
    // Aliyun OSS Android SDK(M2 upload:115 storage layer = Aliyun OSS,见 spec §3.3)
    implementation(libs.aliyun.oss.android)
    // WorkManager(M2 upload 后台 + setProgress)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.paparazzi)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}

// Gradle 9 + Paparazzi 2.0.0-alpha04 兼容性：禁用 HTML test reports
tasks.withType<Test>().configureEach {
    reports.html.required = false
}
