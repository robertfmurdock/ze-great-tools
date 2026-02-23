package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val (firstFingerprint, firstManifest) = runFingerprintWithManifest()
        writeBuildWith(base = baseBuildScript, extra = dependencyChange)
        val (secondFingerprint, secondManifest) = runFingerprintWithManifest()
        listOf(firstFingerprint, secondFingerprint, firstManifest, secondManifest)
    } verify { (firstFingerprint, secondFingerprint, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstFingerprint, secondFingerprint, "Fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when non-test dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
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
        val (firstHash, firstManifest) = runFingerprintWithManifest()

        writeBuildWith(
            base = baseBuildScript,
            extra = dependencyChange,
        )

        val (secondHash, secondManifest) = runFingerprintWithManifest()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstHash, secondHash, "JS fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when JS main dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
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
        val (firstHash, firstManifest) = runFingerprintWithManifest()

        writeBuildWith(
            base = baseBuildScript,
            extra = testDependencyChange,
        )

        val (secondHash, secondManifest) = runFingerprintWithManifest()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after test dependency change")

        assertFingerprintUnchanged(firstHash, secondHash, "Fingerprint should NOT change for test dependencies!")
        assertEquals(
            firstManifest,
            secondManifest,
            "Manifest should NOT change for test-only dependency changes.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `root fingerprint reflects changes in subprojects`() = setup(object {
        lateinit var appBuildFile: File
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

        appBuildFile = testProjectDir.resolve("app/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins { kotlin("multiplatform") version "2.3.0" }
                kotlin { jvm() }
                repositories { mavenCentral() }
                
                """.trimIndent(),
            )
        }
    } exercise {
        val firstHash = runFingerprint()

        appBuildFile.appendText("\n$dependencyChange")

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assert(firstHash != secondHash) { "Root fingerprint must change when subproject dependencies change!" }
    }

    @Test
    fun `fingerprint ignores unconfigured subprojects`() = setup(object {
        lateinit var appBuild: File
        lateinit var ignoredBuild: File
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

        appBuild = testProjectDir.resolve("app/build.gradle.kts").apply { parentFile.mkdirs() }
        ignoredBuild = testProjectDir.resolve("ignored-lib/build.gradle.kts").apply { parentFile.mkdirs() }

        appBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
        ignoredBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
    } exercise {
        val firstHash = runFingerprint()

        ignoredBuild.appendText("\n$dependencyChange")
        val secondHash = runFingerprint()

        appBuild.appendText("\n$dependencyChange")
        val thirdHash = runFingerprint()

        listOf(firstHash, secondHash, thirdHash)
    } verify { (firstHash, secondHash, thirdHash) ->
        assertFingerprintUnchanged(
            firstHash,
            secondHash,
            "Hash should remain stable when unconfigured subprojects change!",
        )
        assertFingerprintChanged(firstHash, thirdHash, "Hash should change when configured subprojects change!")
    }
}
