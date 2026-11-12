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

rootProject.name = "tools"

include("certifier-plugin")
include("cli-tools")
include("dependency-bom")
include("plugin-dependency-bom")
include("digger-core")
include("digger-gradle")
include("digger-guide")
include("digger-json")
include("digger-model")
include("digger-plugin")
include("fingerprint-plugin")
include("git-adapter")
include("git-test")
include("tagger-core")
include("tagger-gradle")
include("tagger-guide")
include("tagger-json")
include("tagger-plugin")
include("tools-bom")
