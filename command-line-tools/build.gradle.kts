
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    base
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.org.jmailen.kotlinter)
    alias(libs.plugins.com.github.ben.manes.versions)
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
}

group = "com.zegreatrob.tools"

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
    register("release") {
        mustRunAfter(check)
        finalizedBy(provider { (getTasksByName("publish", true)).toList() })
        if (!isSnapshot()) {
            dependsOn(provider { (getTasksByName("publishPlugins", true) - this).toList() })
        }
    }
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
        revision = "release"

        rejectVersionIf {
            "^[0-9.]+[0-9](-RC|-M[0-9]+|-RC[0-9]+|-beta.*|-Beta.*|-alpha.*)\$"
                .toRegex(RegexOption.IGNORE_CASE)
                .matches(candidate.version)
        }
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")
