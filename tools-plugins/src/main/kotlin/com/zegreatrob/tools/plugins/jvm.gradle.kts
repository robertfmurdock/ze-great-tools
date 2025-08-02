package com.zegreatrob.tools.plugins

plugins {
    base
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("org.jetbrains.kotlin.jvm")
    signing
    `java-library`
}

kotlin {
    jvmToolchain(17)
}
