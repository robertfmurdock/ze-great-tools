import nl.littlerobots.vcu.plugin.versionSelector


repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    base
    id("org.jetbrains.kotlin.multiplatform") version embeddedKotlinVersion apply false
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
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")

versionCatalogUpdate {
    val rejectRegex = "^[0-9.]+[0-9](-RC|-M[0-9]*|-RC[0-9]*.*|-beta.*|-Beta.*|-alpha.*|-dev.*)$".toRegex()
    versionSelector { versionCandidate ->
        !rejectRegex.matches(versionCandidate.candidate.version)
    }
}
