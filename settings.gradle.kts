pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// JDK Toolchain auto-provisioning:
// 让 Gradle 在 `jvmToolchain(21)` 找不到本机 JDK 21 时，自动从 Adoptium 下载。
// 这样仓库里就不需要硬编码任何本机 JDK 路径（org.gradle.java.home），
// 任何开发者 clone 后第一次构建即可正常工作。
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StarVault"
include(":app")
