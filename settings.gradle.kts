pluginManagement {
    includeBuild("tools-plugins")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.6"
    id("com.zegreatrob.tools.dashboard-test-summary")
}

rootProject.name = "ze-great-tools"
includeBuild("tools-plugins")

dashboardTestSummary {
    includeBuild("tools", "com.zegreatrob.tools:tools")
    includeBuild("tools-tests", "com.zegreatrob.tools:tools-tests")
    includeBuild("command-line-tools", "com.zegreatrob.tools:command-line-tools")
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI").isNullOrBlank().not() }
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        tag("CI")
    }
}

buildCache {
    local {
        isEnabled = true
    }
}
