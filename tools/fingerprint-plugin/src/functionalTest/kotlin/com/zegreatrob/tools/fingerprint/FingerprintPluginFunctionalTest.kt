package com.zegreatrob.tools.fingerprint

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FingerprintPluginFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    @Test
    fun `plugin generates fingerprint file in KMP project`() {
        settingsFile.writeText("rootProject.name = \"kmp-test-project\"")

        buildFile.writeText(
            """
            plugins {
                kotlin("multiplatform") version "1.9.20"
                id("com.zegreatrob.tools.fingerprint")
            }

            kotlin {
                jvm()
            }
            """.trimIndent(),
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .forwardOutput()
            .build()

        val fingerprintFile = testProjectDir.resolve("build/fingerprint.txt")
        assertTrue(fingerprintFile.exists(), "Fingerprint file should be generated at ${fingerprintFile.path}")
    }

    @Test
    fun `fingerprint changes when dependencies are modified`() {
        settingsFile.writeText("rootProject.name = \"dependency-test\"")

        val initialBuildScript = """
        plugins {
            kotlin("multiplatform") version "1.9.20"
            id("com.zegreatrob.tools.fingerprint")
        }
        kotlin { jvm() }
        repositories { mavenCentral() }
        """.trimIndent()

        buildFile.writeText(initialBuildScript)

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .build()

        val fingerprintFile = testProjectDir.resolve("build/fingerprint.txt")
        val firstFingerprint = fingerprintFile.readText()

        buildFile.writeText(
            """
        $initialBuildScript
        
        kotlin {
            sourceSets {
                val commonMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                    }
                }
            }
        }
            """.trimIndent(),
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .build()

        val secondFingerprint = fingerprintFile.readText()

        assert(firstFingerprint != secondFingerprint) {
            "Fingerprint should have changed! Old: $firstFingerprint, New: $secondFingerprint"
        }
    }

    @Test
    fun `fingerprint changes when JS dependencies are modified`() {
        settingsFile.writeText("rootProject.name = \"js-test\"")

        val jsBuildScript = """
        plugins {
            kotlin("multiplatform") version "1.9.20"
            id("com.zegreatrob.tools.fingerprint")
        }
        kotlin {
            js(IR) { browser() }
        }
        repositories { mavenCentral() }
        """.trimIndent()

        buildFile.writeText(jsBuildScript)
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .build()

        val firstHash = testProjectDir.resolve("build/fingerprint.txt").readText()

        buildFile.writeText(
            """
        $jsBuildScript
        kotlin {
            sourceSets {
                val jsMain by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
                    }
                }
            }
        }
            """.trimIndent(),
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .build()

        val secondHash = testProjectDir.resolve("build/fingerprint.txt").readText()

        assert(firstHash != secondHash) { "JS fingerprint should have changed!" }
    }

    @Test
    fun `fingerprint remains identical when test dependencies change`() {
        settingsFile.writeText("rootProject.name = \"test-isolation-check\"")

        val baseBuildScript = """
        plugins {
            kotlin("multiplatform") version "1.9.20"
            id("com.zegreatrob.tools.fingerprint")
        }
        kotlin { jvm() }
        repositories { mavenCentral() }
        """.trimIndent()

        buildFile.writeText(baseBuildScript)
        val firstHash = runFingerprint()

        buildFile.writeText(
            """
        $baseBuildScript
        kotlin {
            sourceSets {
                val commonTest by getting {
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-test")
                    }
                }
            }
        }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        assert(firstHash == secondHash) {
            "Fingerprint should NOT change for test dependencies! Old: $firstHash, New: $secondHash"
        }
    }

    private fun runFingerprint(): String {
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint")
            .withPluginClasspath()
            .build()
        return testProjectDir.resolve("build/fingerprint.txt").readText()
    }

    @Test
    fun `fingerprint changes when plugin version changes`(@TempDir testProjectDir: File) {
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        testProjectDir.resolve("settings.gradle.kts").writeText("")

        val baseBuildScript = """
        plugins {
            kotlin("multiplatform") version "1.9.20"
            id("com.zegreatrob.tools.fingerprint")
        }
        kotlin { jvm() }
        repositories { mavenCentral() }
        """.trimIndent()
        buildFile.writeText(baseBuildScript)

        val hashA = runWithVersion(testProjectDir, "1.0.0")

        val hashB = runWithVersion(testProjectDir, "2.0.0")

        assert(hashA != hashB) { "Fingerprint must change when plugin version updates!" }
    }

    private fun runWithVersion(dir: File, version: String): String {
        GradleRunner.create()
            .withProjectDir(dir)
            .withArguments("generateFingerprint", "-Dtest.plugin.version=$version")
            .withPluginClasspath()
            .build()
        return dir.resolve("build/fingerprint.txt").readText()
    }

    @Test
    fun `task is retrieved from build cache on second run`() {
        val buildScript = """
        plugins {
            kotlin("multiplatform") version "1.9.20"
            id("com.zegreatrob.tools.fingerprint")
        }
        kotlin { jvm() }
        repositories { mavenCentral() }
        """.trimIndent()
        buildFile.writeText(buildScript)

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint", "--build-cache")
            .withPluginClasspath()
            .build()

        testProjectDir.resolve("build").deleteRecursively()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateFingerprint", "--build-cache")
            .withPluginClasspath()
            .build()

        assertEquals(result.task(":generateFingerprint")?.outcome, TaskOutcome.FROM_CACHE)
    }
}
