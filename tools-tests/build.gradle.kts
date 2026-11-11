import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.zegreatrob.tools.dashboard-test-summary")
    base
    id("com.zegreatrob.tools.plugins.lint")
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

group = "com.zegreatrob.tools"

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                allWarningsAsErrors = true
            }
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                allWarningsAsErrors = true
            }
        }
    }
}

tasks {
    assemble { dependsOn(provider { (getTasksByName("assemble", true) - this).toList() }) }
    check { dependsOn(provider { (getTasksByName("check", true) - this).toList() }) }
    clean { dependsOn(provider { (getTasksByName("clean", true) - this).toList() }) }
    register<Delete>("resetYarnLock") {
        group = "build setup"
        description = "Deletes kotlin-js-store/yarn.lock to force fresh transitive dependency resolution"
        delete(file("kotlin-js-store/yarn.lock"))
    }
    matching { it.name == "kotlinUpgradeYarnLock" }.configureEach {
        mustRunAfter("resetYarnLock")
    }
    register("collectResults") {
        group = "verification"
        description = "Collects test results from test modules"
        dependsOn(provider { (getTasksByName("collectResults", true) - this).toList() })
    }
    register("formatKotlin") {
        group = "formatting"
        description = "Applies Kotlin code formatting across all test modules"
        dependsOn(provider { (getTasksByName("formatKotlin", true) - this).toList() })
    }
}
