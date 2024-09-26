pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "cli-tools"

include("digger-cli")
include("tagger-cli")

includeBuild("../tools")
includeBuild("../tools-plugins")
