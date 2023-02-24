repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("com.zegreatrob.tools.tagger")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("nl.littlerobots.version-catalog-update").version("0.7.0")
    base
}

tagger {
    releaseBranch = "main"
}

tasks {
    check {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":check") }.toList() })
    }
    register("publish") {
        dependsOn(provider { gradle.includedBuild("tagger").task(":publish") })
    }
}
