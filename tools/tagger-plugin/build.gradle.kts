@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":dependency-bom")))
    implementation(project(":tagger-gradle"))
    implementation(project(":tagger-guide"))
    implementation(project(":tagger-json"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("tagger") {
            id = "com.zegreatrob.tools.tagger"
            implementationClass = "com.zegreatrob.tools.TaggerPlugin"
            displayName = "Tagger Plugin"
            description =
                "This plugin automates generation of version numbers based on commit messages and git tags."
            tags.addAll("git", "tags", "version", "semver", "github", "release", "commit")
        }
    }
}

tasks {
    withType(Test::class) {
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
