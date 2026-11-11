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

rootProject.name = "command-line-tools"

include("digger-cli")
include("tagger-cli")

dashboardTestSummary {
    includeBuild("../tools", "com.zegreatrob.tools:tools")
    includeBuild("../tools-tests", "com.zegreatrob.tools:tools-tests")
}
