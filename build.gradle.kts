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
        group = "dependencies"
        description = "Updates version catalog entries across all included builds"
        dependsOn(provider { gradle.includedBuilds.map { it.task(":versionCatalogUpdate") }.toList() })
    }
    register("formatKotlin") {
        group = "formatting"
        description = "Applies Kotlin code formatting across all included builds"
        dependsOn(provider { gradle.includedBuilds.map { it.task(":formatKotlin") }.toList() })
    }
    register("resetYarnLock") {
        group = "build setup"
        description = "Deletes all kotlin-js-store/yarn.lock files to force fresh transitive dependency resolution"
        dependsOn(provider {
            listOf(
                gradle.includedBuild("command-line-tools").task(":resetYarnLock"),
                gradle.includedBuild("tools").task(":resetYarnLock"),
                gradle.includedBuild("tools-tests").task(":resetYarnLock"),
            )
        })
    }
    register("kotlinUpgradeYarnLock") {
        group = "build setup"
        description = "Upgrades Yarn lock files for Kotlin/JS dependencies across all included builds"
        mustRunAfter("resetYarnLock")
        dependsOn(provider {
            listOf(
                gradle.includedBuild("command-line-tools").task(":kotlinUpgradeYarnLock"),
                gradle.includedBuild("tools").task(":kotlinUpgradeYarnLock"),
                gradle.includedBuild("tools-tests").task(":kotlinUpgradeYarnLock"),
            )
        })
    }
    val testBuilds = listOf(
        gradle.includedBuild("tools"),
        gradle.includedBuild("tools-tests"),
    )
    register<Copy>("collectResults") {
        group = "verification"
        description = "Collects test results from all test builds into root build directory"
        dependsOn(provider { (getTasksByName("collectResults", true) - this).toList() })
        dependsOn(provider { testBuilds.map { it.task(":collectResults") } })
        from(testBuilds.map { it.projectDir.resolve("build/test-output") })
        into(rootProject.layout.buildDirectory.file("test-output/${project.path}".replace(":", "/")))
    }
    register<Exec>("uploadCliDistributions") {
        group = "publishing"
        description = "Uploads CLI distribution archives to GitHub release (requires TAGGER_VERSION)"
        val versionEnv = providers.environmentVariable("TAGGER_VERSION")
        onlyIf { versionEnv.isPresent && !versionEnv.getOrElse("").contains("SNAPSHOT") }
        dependsOn(
            gradle.includedBuild("command-line-tools").task(":tagger-cli:jvmDistZip"),
            gradle.includedBuild("command-line-tools").task(":tagger-cli:jvmDistZipChecksum"),
            gradle.includedBuild("command-line-tools").task(":digger-cli:jvmDistZip"),
            gradle.includedBuild("command-line-tools").task(":digger-cli:jvmDistZipChecksum"),
        )
        doFirst {
            commandLine(
                "gh", "release", "upload", versionEnv.get(),
                "command-line-tools/tagger-cli/build/distributions/tagger-cli-jvm.zip",
                "command-line-tools/tagger-cli/build/distributions/tagger-cli-jvm.zip.sha256",
                "command-line-tools/digger-cli/build/distributions/digger-cli-jvm.zip",
                "command-line-tools/digger-cli/build/distributions/digger-cli-jvm.zip.sha256",
                "--clobber"
            )
        }
    }
}
