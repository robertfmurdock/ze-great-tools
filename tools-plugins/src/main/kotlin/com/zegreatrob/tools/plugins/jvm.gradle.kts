package com.zegreatrob.tools.plugins

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    base
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("com.zegreatrob.tools.plugins.testmints")
    id("org.jetbrains.kotlin.jvm")
    signing
    `java-library`
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
}
