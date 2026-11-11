pluginManagement {
    includeBuild("../tools-plugins")
    repositories {
        gradlePluginPortal()
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

rootProject.name = "command-line-tools"
includeBuild("../tools")
includeBuild("../tools-tests")

include("digger-cli")
include("tagger-cli")


