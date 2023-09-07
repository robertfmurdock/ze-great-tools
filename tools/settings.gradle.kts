pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("digger")
include("digger-plugin")
include("tagger-plugin")
include("tools-bom")

includeBuild("../tools-plugins")
