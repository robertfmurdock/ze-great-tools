pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("certifier-plugin")
include("dependency-bom")
include("digger-core")
include("digger-json")
include("digger-model")
include("digger-plugin")
include("tagger-plugin")
include("tools-bom")

includeBuild("../tools-plugins")
