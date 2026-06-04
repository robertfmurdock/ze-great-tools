import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.zegreatrob.tools.plugins.publish")
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
    js(IR) {
        nodejs()
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            allWarningsAsErrors = true
        }
    }
}

dependencies {
    commonTestImplementation(platform(libs.com.zegreatrob.testmints.bom))
    commonTestImplementation(kotlin("test"))
    commonTestImplementation(libs.com.zegreatrob.testmints.minassert)
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
    }
}
