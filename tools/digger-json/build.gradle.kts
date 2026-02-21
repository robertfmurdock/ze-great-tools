@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask


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
    js(IR) { nodejs() }
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainImplementation(platform(project(":plugin-dependency-bom")))
    commonMainApi(project(":digger-model"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation(kotlin("test"))
    "commonTestImplementation"("org.jetbrains.kotlin:kotlin-stdlib") {
        version {
            strictly(embeddedKotlinVersion)
        }
    }
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
    }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
