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

buildCache {
    local {
        isEnabled = true
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
