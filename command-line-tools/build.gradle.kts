import nl.littlerobots.vcu.plugin.versionSelector
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    base
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    id("com.zegreatrob.tools.fingerprint")
}

group = "com.zegreatrob.tools"

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        dependencies {
            add("commonTestImplementation", platform(libs.com.zegreatrob.testmints.bom))
        }
        extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                allWarningsAsErrors = true
            }
        }
    }
    plugins.withId("org.jmailen.kotlinter") {
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(tasks.matching { it.name == "lintKotlin" })
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
    register("release") {
        mustRunAfter(check)
        finalizedBy(provider { (getTasksByName("publish", true)).toList() })
        if (!isSnapshot()) {
            dependsOn(provider { (getTasksByName("publishPlugins", true) - this).toList() })
        }
    }
    generateFingerprint {
        digestInputDumpFile.set(layout.buildDirectory.file("fingerprint-dump.txt"))
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")

versionCatalogUpdate {
    val rejectRegex = "^[0-9.]+[0-9](-RC|-M[0-9]*|-RC[0-9]*.*|-beta.*|-Beta.*|-alpha.*|-dev.*)$".toRegex()
    versionSelector { versionCandidate ->
        !rejectRegex.matches(versionCandidate.candidate.version)
    }
}
