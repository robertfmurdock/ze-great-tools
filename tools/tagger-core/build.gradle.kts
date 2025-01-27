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
    commonMainApi(project(":git-adapter"))
    commonMainImplementation(project(":digger-core"))
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    "jvmTestImplementation"(kotlin("test-junit", embeddedKotlinVersion))
}

tasks {
    formatKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
