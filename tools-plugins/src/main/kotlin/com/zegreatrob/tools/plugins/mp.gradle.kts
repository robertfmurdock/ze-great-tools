package com.zegreatrob.tools.plugins

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    base
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("org.jetbrains.kotlin.multiplatform")
    id("com.zegreatrob.tools.plugins.publish")
    signing
}

kotlin {
    jvmToolchain(11)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks {
    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from("${rootDir.absolutePath}/javadocs")
    }
    publishing.publications {
        withType<MavenPublication> { artifact(javadocJar) }
    }
}

afterEvaluate {
    tasks {
        withType<PublishToMavenRepository> {
            mustRunAfter(withType<Sign>())
        }
    }
}
