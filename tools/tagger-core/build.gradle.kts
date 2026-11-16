@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask


plugins {
    id("com.zegreatrob.tools.plugins.library")
}

group = "com.zegreatrob.tools"

kotlin {
    jvm()
    js { nodejs() }
    sourceSets.jsMain.dependencies {
        // kotlinx-datetime 0.8.0 pins 3.2.0. Revisit when its wider range ships.
        implementation(npm("@js-joda/core", "6.3.2"))
    }
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainApi(project(":git-adapter"))
    commonMainImplementation(project(":digger-core"))
    commonMainImplementation(libs.org.jetbrains.kotlinx.kotlinx.datetime)
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    "jvmTestImplementation"(kotlin("test-junit", embeddedKotlinVersion))
}

tasks {
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
