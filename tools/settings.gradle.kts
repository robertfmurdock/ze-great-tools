pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("certifier-plugin")
include("cli-tools")
include("dependency-bom")
include("digger-core")
include("digger-json")
include("digger-model")
include("digger-plugin")
include("digger-test")
include("fingerprint-plugin")
include("git-adapter")
include("git-test")
include("tagger-core")
include("tagger-json")
include("tagger-plugin")
include("tagger-test")
include("tools-bom")

includeBuild("../tools-plugins")
