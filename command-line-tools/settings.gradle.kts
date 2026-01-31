pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    includeBuild("../build-logic")
}

rootProject.name = "command-line-tools"

include("digger-cli")
include("tagger-cli")

includeBuild("../tools")