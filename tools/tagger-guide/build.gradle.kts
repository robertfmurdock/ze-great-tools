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
