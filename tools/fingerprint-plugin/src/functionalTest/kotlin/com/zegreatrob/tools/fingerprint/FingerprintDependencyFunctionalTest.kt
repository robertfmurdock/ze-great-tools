package com.zegreatrob.tools.fingerprint

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.junit.jupiter.api.Test

class FingerprintDependencyFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `fingerprint changes when dependencies are modified`() = setup(object {
        val baseBuildScript = kmpBuild()
        val dependencyChange = """
            kotlin {
                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                        }
                    }
                }
            }
        """.trimIndent()
    }) {
        writeSettings("dependency-test")
        writeBuild(baseBuildScript)
    } exercise {
        val baseline = runFingerprintWithManifest()
        writeBuildWith(base = baseBuildScript, extra = dependencyChange)
        val afterChange = runFingerprintWithManifest()
        Pair(baseline, afterChange)
    } verify { (baseline, afterChange) ->
        assertManifestContainsDependencyIngredients(baseline.manifest, context = "baseline")
        assertManifestContainsDependencyIngredients(afterChange.manifest, context = "after dependency change")

        assertFingerprintChanged(baseline.hash, afterChange.hash, "Fingerprint should have changed!")
        (baseline.manifest != afterChange.manifest).assertIsEqualTo(
            true,
            "Manifest should change when non-test dependencies change.\n--- first ---\n${baseline.manifest}\n--- second ---\n${afterChange.manifest}",
        )
    }

    @Test
    fun `fingerprint changes when JS dependencies are modified`() = setup(object {
        val baseBuildScript = kmpBuild(
            kotlinBlock = """
                kotlin {
                    js(IR) { browser() }
                }
            """.trimIndent(),
        )
        val dependencyChange = """
            kotlin {
                sourceSets {
                    val jsMain by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
                        }
                    }
                }
            }
        """.trimIndent()
    }) {
        writeSettings("js-test")

        writeBuild(baseBuildScript)
    } exercise {
        val baseline = runFingerprintWithManifest()

        writeBuildWith(
            base = baseBuildScript,
            extra = dependencyChange,
        )

        val afterChange = runFingerprintWithManifest()

        Pair(baseline, afterChange)
    } verify { (baseline, afterChange) ->
        assertManifestContainsDependencyIngredients(baseline.manifest, context = "baseline")
        assertManifestContainsDependencyIngredients(afterChange.manifest, context = "after dependency change")

        assertFingerprintChanged(baseline.hash, afterChange.hash, "JS fingerprint should have changed!")
        (baseline.manifest != afterChange.manifest).assertIsEqualTo(
            true,
            "Manifest should change when JS main dependencies change.\n--- first ---\n${baseline.manifest}\n--- second ---\n${afterChange.manifest}",
        )
    }

    @Test
    fun `fingerprint remains identical when test dependencies change`() = setup(object {
        val baseBuildScript = kmpBuild()
        val testDependencyChange = """
            kotlin {
                sourceSets {
                    val commonTest by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-test")
                        }
                    }
                }
            }
        """.trimIndent()
    }) {
        writeSettings("test-isolation-check")

        writeBuild(baseBuildScript)
    } exercise {
        val baseline = runFingerprintWithManifest()

        writeBuildWith(
            base = baseBuildScript,
            extra = testDependencyChange,
        )

        val afterChange = runFingerprintWithManifest()

        Pair(baseline, afterChange)
    } verify { (baseline, afterChange) ->
        assertManifestContainsDependencyIngredients(baseline.manifest, context = "baseline")
        assertManifestContainsDependencyIngredients(afterChange.manifest, context = "after test dependency change")

        assertFingerprintUnchanged(baseline.hash, afterChange.hash, "Fingerprint should NOT change for test dependencies!")
        afterChange.manifest.assertIsEqualTo(
            baseline.manifest,
            "Manifest should NOT change for test-only dependency changes.\n--- first ---\n${baseline.manifest}\n--- second ---\n${afterChange.manifest}",
        )
    }

    @Test
    fun `root fingerprint reflects changes in subprojects`() = setup(object {
        val appBuildFile = fileUnderProject("app/build.gradle.kts")
        val dependencyChange = """
            dependencies { "commonMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
        """.trimIndent()
    }) {
        settingsFile.writeText(
            """
            rootProject.name = "multi-project-root"
            include(":app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("multiplatform") version "2.3.0" apply false }
            """.trimIndent(),
        )

        appBuildFile.writeText(
            """
            plugins { kotlin("multiplatform") version "2.3.0" }
            kotlin { jvm() }
            repositories { mavenCentral() }
            
            """.trimIndent(),
        )
    } exercise {
        val baselineHash = runFingerprint()

        appBuildFile.appendText("\n$dependencyChange")

        val afterChangeHash = runFingerprint()

        Pair(baselineHash, afterChangeHash)
    } verify { (baselineHash, afterChangeHash) ->
        (baselineHash != afterChangeHash).assertIsEqualTo(
            true,
            "Root fingerprint must change when subproject dependencies change!",
        )
    }

    @Test
    fun `fingerprint ignores unconfigured subprojects`() = setup(object {
        val appBuild = fileUnderProject("app/build.gradle.kts")
        val ignoredBuild = fileUnderProject("ignored-lib/build.gradle.kts")
        val dependencyChange =
            """dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }"""
    }) {
        settingsFile.writeText(
            """
            rootProject.name = "filter-test"
            include(":app", ":ignored-lib")
            """.trimIndent(),
        )

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("jvm") version "2.3.0" apply false }
            repositories { mavenCentral() }
            fingerprintConfig {
                includedProjects.add("app")
            }
            """.trimIndent(),
        )

        appBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
        ignoredBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
    } exercise {
        val baselineHash = runFingerprint()

        ignoredBuild.appendText("\n$dependencyChange")
        val ignoredChangeHash = runFingerprint()

        appBuild.appendText("\n$dependencyChange")
        val configuredChangeHash = runFingerprint()

        listOf(baselineHash, ignoredChangeHash, configuredChangeHash)
    } verify { (baselineHash, ignoredChangeHash, configuredChangeHash) ->
        assertFingerprintUnchanged(
            baselineHash,
            ignoredChangeHash,
            "Hash should remain stable when unconfigured subprojects change!",
        )
        assertFingerprintChanged(baselineHash, configuredChangeHash, "Hash should change when configured subprojects change!")
    }
}
