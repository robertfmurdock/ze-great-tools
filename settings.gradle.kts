plugins {
    `gradle-enterprise`
}

rootProject.name = "ze-great-tools"
include("lib")
includeBuild("tagger")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}