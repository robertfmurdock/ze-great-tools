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
    register("resetYarnLock") {
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
        dependsOn(provider { (getTasksByName("collectResults", true) - this).toList() })
        dependsOn(provider { testBuilds.map { it.task(":collectResults") } })
        from(testBuilds.map { it.projectDir.resolve("build/test-output") })
        into(rootProject.layout.buildDirectory.file("test-output/${project.path}".replace(":", "/")))
    }
    register<Exec>("cleanupNpmSnapshots") {
        group = "publishing"
        description = "Deprecate snapshot versions from npm packages"

        val packages = providers.gradleProperty("npmPackages")
            .orElse("@continuous-excellence/tagger,@continuous-excellence/digger")

        inputs.property("packages", packages)

        commandLine("sh", "-c", """
            set -e
            IFS=',' read -ra PACKAGES <<< "${packages.get()}"
            for package in "${'$'}{PACKAGES[@]}"; do
                echo "Processing package: ${'$'}package"

                # Get all versions
                versions=${'$'}(npm view "${'$'}package" versions --json 2>/dev/null || echo "[]")

                # Filter and deprecate snapshot versions
                echo "${'$'}versions" | jq -r '.[]' | while read -r version; do
                    if [[ "${'$'}version" == *"SNAPSHOT"* ]]; then
                        echo "Deprecating ${'$'}package@${'$'}version"
                        npm deprecate "${'$'}package@${'$'}version" "Snapshot version - use latest release instead" || true
                    fi
                done
            done
        """.trimIndent())

        doFirst {
            val nodeAuthToken = System.getenv("NODE_AUTH_TOKEN")
            if (nodeAuthToken.isNullOrBlank()) {
                throw GradleException("NODE_AUTH_TOKEN environment variable is required for npm operations")
            }
        }
    }
}
