@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi


plugins {
    id("com.zegreatrob.tools.plugins.library")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
}

group = "com.zegreatrob.tools"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        compilerOptions { target = "es2015" }
        nodejs()
    }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainApi(project(":digger-model"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation(kotlin("test"))
    "commonTestImplementation"("org.jetbrains.kotlin:kotlin-stdlib") {
        version {
            strictly(embeddedKotlinVersion)
        }
    }
    commonTestImplementation("com.benasher44:uuid")
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
    }
    formatKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
