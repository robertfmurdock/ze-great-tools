@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":digger-gradle"))
    implementation(project(":digger-guide"))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("digger") {
            id = "com.zegreatrob.tools.digger"
            implementationClass = "com.zegreatrob.tools.DiggerPlugin"
            displayName = "Digger Plugin"
            description =
                "This plugin finds and extracts information from git commits, including co-author data."
            tags.addAll("git", "co-author", "pair", "mob", "commit")
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
