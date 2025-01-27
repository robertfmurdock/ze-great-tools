@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi


plugins {
    id("com.zegreatrob.tools.plugins.library")
}

group = "com.zegreatrob.tools"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
        compilerOptions { target = "es2015" }
    }
}

dependencies {
    commonMainApi(platform(project(":dependency-bom")))
    commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime")
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    commonTestImplementation(project(":git-test"))
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    formatKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
