plugins {
    `gradle-enterprise`
}

rootProject.name = "ze-great-tools"
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
