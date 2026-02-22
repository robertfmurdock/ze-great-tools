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

includeBuild("../tools")
