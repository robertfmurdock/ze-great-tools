pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("certifier-plugin")
include("dependency-bom")
include("digger-cli")
include("digger-core")
include("digger-json")
include("digger-model")
include("digger-plugin")
include("digger-test")
include("git-adapter")
include("git-test")
include("tagger-core")
include("tagger-plugin")
include("tagger-test")
include("tools-bom")

includeBuild("../tools-plugins")
