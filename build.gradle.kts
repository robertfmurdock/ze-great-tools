repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    id("com.zegreatrob.tools.tagger")
    id("com.zegreatrob.tools.digger")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.fingerprint")
    base
}

tagger {
    releaseBranch = "main"
    githubReleaseEnabled.set(true)
    userName = "github-actions[bot]"
    userEmail = "6215634+robertfmurdock@users.noreply.github.com"
}

fingerprintConfig {
    includedBuilds = listOf("command-line-tools", "tools")
}

tasks {
    assemble {
        dependsOn(aggregateFingerprints)
        dependsOn(provider { gradle.includedBuilds.map { it.task(":assemble") }.toList() })
    }
    release {
        dependsOn("check")
        dependsOn(provider { gradle.includedBuild("command-line-tools").task(":release") })
        dependsOn(provider { gradle.includedBuild("tools").task(":release") })
        finalizedBy(currentContributionData)
    }
    check {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":check") }.toList() })
    }
    clean {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":clean") }.toList() })
    }
    register("versionCatalogUpdate") {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":versionCatalogUpdate") }.toList() })
    }
    register("formatKotlin") {
        dependsOn(provider { gradle.includedBuilds.map { it.task(":formatKotlin") }.toList() })
    }
    val testBuilds = listOf(
        gradle.includedBuild("tools"),
        gradle.includedBuild("tools-tests"),
    )
    register<Copy>("collectResults") {
        dependsOn(provider { (getTasksByName("collectResults", true) - this).toList() })
        dependsOn(provider { testBuilds.map { it.task(":collectResults") } })
        from(testBuilds.map { it.projectDir.resolve("build/test-output") })
        into(rootProject.layout.buildDirectory.file("test-output/${project.path}".replace(":", "/")))
    }
}
