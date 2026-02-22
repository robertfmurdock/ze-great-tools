pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools-tests"

include("tagger-test")
include("tagger-core-test")
include("tagger-plugin-test")
include("digger-core-test")
include("digger-json-test")

includeBuild("../tools")
