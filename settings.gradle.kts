pluginManagement {
    includeBuild("tools-plugins")
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
    id("com.gradle.develocity") version "3.17.6"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ze-great-tools"
includeBuild("tools-plugins")
includeBuild("tools")
includeBuild("tools-tests")
includeBuild("command-line-tools")

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI").isNullOrBlank().not() }
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        tag("CI")
    }
}

buildCache {
    local {
        isEnabled = true
    }
}
