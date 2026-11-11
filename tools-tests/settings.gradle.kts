pluginManagement {
    includeBuild("../tools-plugins")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
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


