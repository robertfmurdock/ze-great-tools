repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("com.zegreatrob.tools.plugins.versioning")
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    base
}

tasks {
    check { dependsOn(provider { (getTasksByName("check", true) - this).toList() }) }
    register("release"){ finalizedBy(provider { (getTasksByName("publish", true)).toList() }) }
}

versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        keepUnusedVersions.set(true)
        keepUnusedLibraries.set(true)
    }
}
