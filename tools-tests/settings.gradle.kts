pluginManagement {
    includeBuild("../tools-plugins")
    repositories {
        gradlePluginPortal {
            content {
                includeGroupAndSubgroups("org.jetbrains.kotlin")
                includeGroupAndSubgroups("org.jmailen.gradle")
                includeGroup("org.jmailen.kotlinter")
                includeGroupAndSubgroups("nl.littlerobots")
                includeGroupAndSubgroups("io.sdkman")
                includeGroupAndSubgroups("org.gradle.crypto")
                includeGroup("gradle.plugin.org.gradle.crypto")
                includeGroup("org.gradle.plugin")
                includeGroupAndSubgroups("io.github.gradle")
                includeGroup("io.github.gradle-nexus")
                includeGroup("io.github.gradle-nexus.publish-plugin")
                includeGroupAndSubgroups("com.gradle")
                includeGroup("com.gradle.plugin-publish")
            }
        }
        mavenCentral()
    }
}

plugins {
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "tools-tests"
includeBuild("../tools")

include("tagger-test")
include("tagger-core-test")
include("tagger-plugin-test")
include("digger-core-test")
include("digger-json-test")
include("digger-test")
include("digger-plugin-test")


