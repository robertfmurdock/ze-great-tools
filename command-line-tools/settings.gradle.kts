pluginManagement {
    includeBuild("../tools-plugins")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "command-line-tools"

include("digger-cli")
include("tagger-cli")

includeBuild("../tools")
includeBuild("../tools-tests")
