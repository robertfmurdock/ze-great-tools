pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("dependency-bom")
include("digger-core")
include("digger-model")
include("digger-json")
include("digger-plugin")
include("tagger-plugin")
include("tools-bom")

includeBuild("../tools-plugins")
