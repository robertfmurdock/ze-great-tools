pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    `gradle-enterprise`
}

rootProject.name = "ze-great-tools"
includeBuild("tools-plugins")
includeBuild("tools")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

buildCache {
    local {
        isEnabled = true
    }
}
