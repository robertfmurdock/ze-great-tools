@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
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
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
    testImplementation(libs.com.zegreatrob.testmints.minassert)
    testImplementation(libs.com.zegreatrob.testmints.standard)
    "functionalTestImplementation"(platform(libs.org.junit.junit.bom))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("certifier") {
            id = "com.zegreatrob.tools.certifier"
            implementationClass = "com.zegreatrob.tools.CertifierPlugin"
            displayName = "Certifier Plugin"
            description =
                "This plugin assists in the installation of certificates into JDKs with Java Keytool."
            tags.addAll("certificates", "keytool", "java")
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
    named<Test>("test") {
        useJUnitPlatform()
    }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
