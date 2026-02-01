package com.zegreatrob.tools.fingerprint

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FingerprintPluginFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    private fun writeSettings(name: String? = null) {
        settingsFile.writeText(
            name?.let { """rootProject.name = "$it"""" } ?: "",
        )
    }

    private fun writeBuild(script: String) {
        buildFile.writeText(script.trimIndent())
    }

    private fun gradle(
        projectDir: File = testProjectDir,
        vararg arguments: String,
        forwardOutput: Boolean = false,
        expectFailure: Boolean = false,
    ) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*arguments)
        .withPluginClasspath()
        .apply { if (forwardOutput) forwardOutput() }
        .let { runner -> if (expectFailure) runner.buildAndFail() else runner.build() }

    private fun fingerprintFile(dir: File = testProjectDir) = dir.resolve("build/fingerprint.txt")

    private fun runFingerprint(dir: File = testProjectDir, vararg extraArgs: String): String {
        gradle(dir, "generateFingerprint", "--configuration-cache", *extraArgs)
        return fingerprintFile(dir).readText()
    }

    private fun kmpBuild(
        kotlinBlock: String = "kotlin { jvm() }",
        repositoriesBlock: String = "repositories { mavenCentral() }",
    ) = """
        plugins {
            kotlin("multiplatform") version "2.3.0"
            id("com.zegreatrob.tools.fingerprint")
        }
        $repositoriesBlock
        $kotlinBlock
    """.trimIndent()

    @Test
    fun `plugin generates fingerprint file in KMP project`() {
        writeSettings("kmp-test-project")

        writeBuild(
            kmpBuild(
                kotlinBlock = """
                    kotlin {
                        jvm()
                    }
                """.trimIndent(),
            ),
        )

        gradle(arguments = arrayOf("generateFingerprint", "--configuration-cache"), forwardOutput = true)

        val fingerprintFile = fingerprintFile()
        assertTrue(fingerprintFile.exists(), "Fingerprint file should be generated at ${fingerprintFile.path}")
    }

    @Test
    fun `fingerprint changes when dependencies are modified`() {
        writeSettings("dependency-test")

        val base = kmpBuild()
        writeBuild(base)

        val firstFingerprint = runFingerprint()

        writeBuild(
            """
            $base

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

        val secondFingerprint = runFingerprint()

        assert(firstFingerprint != secondFingerprint) {
            "Fingerprint should have changed! Old: $firstFingerprint, New: $secondFingerprint"
        }
    }

    @Test
    fun `fingerprint changes when JS dependencies are modified`() {
        writeSettings("js-test")

        val base = kmpBuild(
            kotlinBlock = """
                kotlin {
                    js(IR) { browser() }
                }
            """.trimIndent(),
        )
        writeBuild(base)

        val firstHash = runFingerprint()

        writeBuild(
            """
            $base
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

        val secondHash = runFingerprint()

        assert(firstHash != secondHash) { "JS fingerprint should have changed!" }
    }

    @Test
    fun `fingerprint remains identical when test dependencies change`() {
        writeSettings("test-isolation-check")

        val base = kmpBuild()
        writeBuild(base)

        val firstHash = runFingerprint()

        writeBuild(
            """
            $base
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

    @Test
    fun `fingerprint changes when plugin version changes`(@TempDir testProjectDir: File) {
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        testProjectDir.resolve("settings.gradle.kts").writeText("")

        val baseBuildScript = kmpBuild()
        buildFile.writeText(baseBuildScript)

        val hashA = runWithVersion(testProjectDir, "1.0.0")
        val hashB = runWithVersion(testProjectDir, "2.0.0")

        assert(hashA != hashB) { "Fingerprint must change when plugin version updates!" }
    }

    private fun runWithVersion(dir: File, version: String): String {
        gradle(dir, "generateFingerprint", "-Dtest.plugin.version=$version")
        return fingerprintFile(dir).readText()
    }

    @Test
    fun `task is retrieved from build cache on second run`() {
        writeBuild(kmpBuild())

        gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))

        testProjectDir.resolve("build").deleteRecursively()

        val result = gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))

        assertEquals(result.task(":generateFingerprint")?.outcome, TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `root fingerprint reflects changes in subprojects`() {
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

        val appBuildFile = testProjectDir.resolve("app/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
            plugins { kotlin("multiplatform") version "2.3.0" }
            kotlin { jvm() }
            repositories { mavenCentral() }
            
                """.trimIndent(),
            )
        }

        val firstHash = runFingerprint()

        appBuildFile.appendText(
            """
        dependencies { "commonMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        assert(firstHash != secondHash) { "Root fingerprint must change when subproject dependencies change!" }
    }

    @Test
    fun `fingerprint ignores unconfigured subprojects`() {
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

        val appBuild = testProjectDir.resolve("app/build.gradle.kts").apply { parentFile.mkdirs() }
        val ignoredBuild = testProjectDir.resolve("ignored-lib/build.gradle.kts").apply { parentFile.mkdirs() }

        appBuild.writeText("plugins { kotlin(\"jvm\") version \"2.3.0\" } repositories { mavenCentral() }")
        ignoredBuild.writeText("plugins { kotlin(\"jvm\") version \"2.3.0\" } repositories { mavenCentral() }")

        val firstHash = runFingerprint()

        ignoredBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val secondHash = runFingerprint()

        assert(firstHash == secondHash) { "Hash should remain stable when unconfigured subprojects change!" }

        appBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val thirdHash = runFingerprint()

        assert(firstHash != thirdHash) { "Hash should change when configured subprojects change!" }
    }

    @Test
    fun `fingerprint fails if dependencies cannot be resolved`() {
        writeSettings("resolution-failure-test")
        writeBuild(
            """
        plugins {
            kotlin("jvm") version "2.3.0"
            id("com.zegreatrob.tools.fingerprint")
        }
        // NO repositories defined
        dependencies {
            implementation("com.example:fake-lib:1.0.0")
        }
            """.trimIndent(),
        )

        val result = gradle(arguments = arrayOf("generateFingerprint"), expectFailure = true)

        assertTrue(result.output.contains("Could not resolve"), result.output)
    }

    @Test
    fun `aggregateFingerprints combines hashes from included builds`(@TempDir testProjectDir: File) {
        val includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"my-included-lib\"")
        includedDir.resolve("build.gradle.kts").writeText(
            """
        plugins { id("com.zegreatrob.tools.fingerprint") }

        val includedMarker = file("included-marker.txt").apply { writeText("lib-content") }

        tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
            pluginVersion.set("1.0")
            classpath.setFrom(files(includedMarker))
        }
            """.trimIndent(),
        )

        val mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
        rootProject.name = "main-app"
        includeBuild("../my-included-lib")
            """.trimIndent(),
        )

        mainDir.resolve("build.gradle.kts").writeText(
            """
        plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("multiplatform") version "2.3.0" apply false }

        val mainMarker = file("main-marker.txt").apply { writeText("app-content") }

        tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
            pluginVersion.set("1.0")
            classpath.setFrom(files(mainMarker))
        }
            """.trimIndent(),
        )

        gradle(mainDir, ":my-included-lib:generateFingerprint", "aggregateFingerprints")

        val localHash = fingerprintFile(mainDir).readText()
        val includedHash = fingerprintFile(includedDir).readText()
        val aggregateHash = mainDir.resolve("build/aggregate-fingerprint.txt").readText()

        assert(aggregateHash != localHash) { "Aggregate should not match local hash" }
        assert(aggregateHash != includedHash) { "Aggregate should not match included hash" }
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`(@TempDir testProjectDir: File) {
        val includedDir = testProjectDir.resolve("my-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"my-lib\"")
        includedDir.resolve("build.gradle.kts").writeText(
            """
        plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        val mainDir = testProjectDir.resolve("my-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
        rootProject.name = "my-app"
        includeBuild("../my-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
        plugins { id("com.zegreatrob.tools.fingerprint") }
        fingerprintConfig {
            includedBuilds.add("my-lib") 
        }
            """.trimIndent(),
        )

        val result = gradle(mainDir, "aggregateFingerprints")

        val includedTaskResult = result.task(":my-lib:generateFingerprint")
        assertNotNull(includedTaskResult, "Task in included build should have been triggered")
        assertTrue(
            includedTaskResult.outcome == TaskOutcome.SUCCESS || includedTaskResult.outcome == TaskOutcome.UP_TO_DATE,
            "Included task should have executed successfully",
        )
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`(@TempDir testProjectDir: File) {
        val nakedDir = testProjectDir.resolve("naked-lib").apply { mkdirs() }
        nakedDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"naked-lib\"")
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        val mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
        rootProject.name = "main-app"
        includeBuild("../naked-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
        plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        val result = gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")

        assertTrue(result.output.contains("SUCCESS"), "Build should succeed even with non-plugin included builds")
        val aggregateFile = mainDir.resolve("build/aggregate-fingerprint.txt")
        assertTrue(aggregateFile.exists(), "Aggregate file should still be generated")
    }
}
