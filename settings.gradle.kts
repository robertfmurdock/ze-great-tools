pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.6"
}

rootProject.name = "ze-great-tools"
includeBuild("tools-plugins")
includeBuild("tools")
includeBuild("cli-tools")

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
