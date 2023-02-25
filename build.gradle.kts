repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("com.zegreatrob.tools.tagger")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    base
}

tagger {
    releaseBranch = "main"
}

tasks {
    check {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":check") }.toList() })
    }
    release {
        dependsOn(provider { gradle.includedBuild("tagger").task(":release") })
    }
    "versionCatalogUpdate" {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":versionCatalogUpdate") }.toList() })
    }
}

versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        keepUnusedPlugins.set(true)
        keepUnusedVersions.set(true)
        keepUnusedLibraries.set(true)
    }
}
