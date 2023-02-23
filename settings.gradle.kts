plugins {
    `gradle-enterprise`
}

rootProject.name = "ze-great-tools"
include("lib")
includeBuild("tools-plugins")
includeBuild("tagger")

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

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
