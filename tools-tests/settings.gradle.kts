pluginManagement {
    includeBuild("../tools-plugins")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.zegreatrob.tools.dashboard-test-summary")
}

rootProject.name = "tools-tests"

include("tagger-test")
include("tagger-core-test")
include("tagger-plugin-test")
include("digger-core-test")
include("digger-json-test")
include("digger-test")
include("digger-plugin-test")

dashboardTestSummary {
    includeBuild("../tools", "com.zegreatrob.tools:tools")
}
