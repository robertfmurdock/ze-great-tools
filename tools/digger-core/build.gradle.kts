@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask


plugins {
    id("com.zegreatrob.tools.plugins.library")
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
    commonMainApi(project(":digger-model"))
    commonMainApi(project(":git-adapter"))
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
    "jvmTestImplementation"(libs.com.zegreatrob.testmints.standard)
    "jvmTestImplementation"(project(":digger-test"))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
