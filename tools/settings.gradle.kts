pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "tools"

include("tagger-plugin")
include("tools-bom")
includeBuild("../tools-plugins")
