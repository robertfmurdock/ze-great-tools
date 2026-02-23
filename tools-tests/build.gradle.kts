import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
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
    apply(plugin = "base")
    apply(plugin = "com.zegreatrob.tools.plugins.lint")
    apply(plugin = "com.zegreatrob.tools.plugins.reports")

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
    register("collectResults") {
        dependsOn(provider { (getTasksByName("collectResults", true) - this).toList() })
    }
    register("formatKotlin") {
        dependsOn(provider { (getTasksByName("formatKotlin", true) - this).toList() })
    }
}
