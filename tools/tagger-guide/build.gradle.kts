import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("com.zegreatrob.tools.plugins.library")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            allWarningsAsErrors = true
        }
    }
    js {
        nodejs()
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            allWarningsAsErrors = true
        }
    }
}
