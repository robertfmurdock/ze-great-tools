@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask


plugins {
    id("com.zegreatrob.tools.plugins.library")
}

group = "com.zegreatrob.tools"

kotlin {
    jvm()
    js {
        nodejs {
            testTask {
                useMocha { timeout = "10s" }
                environment("GIT_CONFIG_GLOBAL", "/dev/null")
                environment("GIT_CONFIG_SYSTEM", "/dev/null")
                environment("GIT_CONFIG_COUNT", "0")
                environment("GIT_CONFIG_PARAMETERS", "")
            }
        }
    }
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainApi(platform(project(":plugin-dependency-bom")))
    commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime")
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    commonTestImplementation(project(":git-test"))
    commonTestImplementation(libs.testmints.minassert)
    commonTestImplementation(libs.testmints.standard)
    commonTestImplementation(libs.testmints.async)
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
        environment("GIT_CONFIG_COUNT", "0")
        environment("GIT_CONFIG_PARAMETERS", "")
    }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
