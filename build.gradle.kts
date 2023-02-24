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
    `maven-publish`
    signing
    alias(libs.plugins.io.github.gradle.nexus.publish.plugin)
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
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set("59331990bed4c")
        }
    }
}