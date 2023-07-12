repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("com.zegreatrob.tools.tagger")
    id("com.zegreatrob.tools.plugins.lint")
    base
}

tagger {
    releaseBranch = "main"
    githubReleaseEnabled.set(true)
}

tasks {
    assemble {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":assemble") }.toList() })
    }
    check {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":check") }.toList() })
    }
    release {
        dependsOn(provider { gradle.includedBuild("tools").task(":release") })
    }
    create("versionCatalogUpdate") {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":versionCatalogUpdate") }.toList() })
    }
    register("formatKotlin") {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":formatKotlin") }.toList() })
    }
}
