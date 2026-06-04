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

testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            gradlePlugin.testSourceSets(sources)
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
    compileOnly(gradleApi())
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
    testImplementation(libs.com.zegreatrob.testmints.minassert)
    testImplementation(libs.com.zegreatrob.testmints.standard)
    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(platform(libs.org.junit.junit.bom))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("fingerprint") {
            id = "com.zegreatrob.tools.fingerprint"
            implementationClass = "com.zegreatrob.tools.FingerprintPlugin"
            displayName = "Fingerprint Plugin"
            description = "This plugin will generate a fingerprint based on all the production code in a project. This is in early development, buyer beware."
            tags.addAll("git", "changes", "dependencies", "fingerprint")
        }
    }
}

tasks {
    "compileFunctionalTestKotlin" {
        dependsOn("compileKotlin")
    }
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
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
    withType<Jar> {
        manifest {
            attributes["Implementation-Version"] = project.version
        }
    }
}
