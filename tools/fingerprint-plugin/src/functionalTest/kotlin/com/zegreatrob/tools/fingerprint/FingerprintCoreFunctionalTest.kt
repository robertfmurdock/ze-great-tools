package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FingerprintCoreFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `plugin generates fingerprint file in KMP project`() = setup(object {
        val projectName = "kmp-test-project"
        val buildScript = kmpBuild(
            kotlinBlock = """
                kotlin {
                    jvm()
                }
            """.trimIndent(),
        )
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint", "--configuration-cache"), forwardOutput = true)
    } verify {
        val fingerprintFile = fingerprintFile()
        assertTrue(fingerprintFile.exists(), "Fingerprint file should be generated at ${fingerprintFile.path}")
        assertFingerprintManifestGeneratedCorrectly()
    }

    @Test
    fun `fingerprint does not change when test source code is modified`() = setup(object {
        val testSourcePath = "src/commonTest/kotlin/ExampleTest.kt"
        val updatedTestSource = """
            package example

            class ExampleTest {
                fun value(): Int = 2
            }
        """.trimIndent()
        val testSourceFile = fileUnderProject(testSourcePath)
    }) {
        writeSettings("test-source-change-test")
        writeBuild(kmpBuild())

        testSourceFile.writeText(
            """
            package example

            class ExampleTest {
                fun value(): Int = 1
            }
            """.trimIndent(),
        )
    } exercise {
        runFingerprintTwiceAfter(change = { testSourceFile.writeText(updatedTestSource) })
    } verify { (baselineHash, afterChangeHash) ->
        assertFingerprintUnchanged(baselineHash, afterChangeHash, "Fingerprint should NOT change when test sources change!")
    }

    @Test
    fun `fingerprint does not include junit when junit is test-only dependency`() = setup(object {
        val projectName = "junit-test-only-dependency-should-not-leak"
        val buildScript = """
            plugins {
                kotlin("jvm") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
            }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        runFingerprint()
        fingerprintManifestFile().readText()
    } verify { manifest ->
        assertManifestContainsDependencyIngredients(manifest, "sanity check: we should have at least one classpath entry")

        val classpathLines = manifest.lineSequence().filter { it.startsWith("classpath|") }.toList()
        val junitClasspathLines = classpathLines.filter { it.contains("junit", ignoreCase = true) }

        assertTrue(
            junitClasspathLines.isEmpty(),
            """
            JUnit must not be treated as a production dependency ingredient when it's only declared in testImplementation.
            Found classpath lines containing 'junit':
            ${junitClasspathLines.joinToString("\n")}
            
            Full manifest:
            $manifest
            """.trimIndent(),
        )
    }

    @Test
    fun `fingerprint changes when module source code is modified`() = setup(object {
        val sourcePath = "src/commonMain/kotlin/Example.kt"
        val updatedSource = """
            package example

            class Example {
                fun value(): Int = 2
            }
        """.trimIndent()
        val sourceFile = fileUnderProject(sourcePath)
    }) {
        writeSettings("source-change-test")
        writeBuild(kmpBuild())

        sourceFile.writeText(
            """
            package example

            class Example {
                fun value(): Int = 1
            }
            """.trimIndent(),
        )
    } exercise {
        val baseline = runFingerprintWithManifest()

        sourceFile.writeText(updatedSource)

        val afterChange = runFingerprintWithManifest()

        Pair(baseline, afterChange)
    } verify { (baseline, afterChange) ->
        assertTrue(baseline.manifest.contains("source|$sourcePath|"))
        assertTrue(afterChange.manifest.contains("source|$sourcePath|"))
        assertFingerprintChanged(baseline.hash, afterChange.hash, "Fingerprint should change when module source changes!")
    }

    @Test
    fun `fingerprint changes when plugin version changes`() = setup(object {
        val oldVersion = "1.0.0"
        val newVersion = "2.0.0"
        val baseBuildScript = kmpBuild()
    }) {
        testProjectDir.resolve("settings.gradle.kts").writeText("")
        testProjectDir.resolve("build.gradle.kts").writeText(baseBuildScript)
    } exercise {
        val hashA = runWithVersion(testProjectDir, oldVersion)
        val hashB = runWithVersion(testProjectDir, newVersion)
        Pair(hashA, hashB)
    } verify { (hashA, hashB) ->
        assert(hashA != hashB) { "Fingerprint must change when plugin version updates!" }
    }

    private fun runWithVersion(dir: File, version: String): String {
        gradle(dir, "generateFingerprint", "-Dtest.plugin.version=$version")
        assertFingerprintManifestGeneratedCorrectly(dir, expectedPluginVersion = version)
        return fingerprintFile(dir).readText()
    }

    @Test
    fun `task is retrieved from build cache on second run`() = setup(object {
        val buildScript = kmpBuild()
    }) {
        writeProject(buildScript = buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        val firstManifest = fingerprintManifestFile().readText()

        testProjectDir.resolve("build").deleteRecursively()

        val result = gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        val secondManifest = fingerprintManifestFile().readText()

        CacheRunResult(result, firstManifest, secondManifest)
    } verify { data ->
        assertTrue(data.firstManifest.isNotBlank())
        assertTrue(data.secondManifest.isNotBlank())
        assertEquals(data.result.task(":generateFingerprint")?.outcome, TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `fingerprint fails if dependencies cannot be resolved`() = setup(object {
        val projectName = "resolution-failure-test"
        val buildScript = """
            plugins {
                kotlin("jvm") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }
            // NO repositories defined
            dependencies {
                implementation("com.example:fake-lib:1.0.0")
            }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint"), expectFailure = true)
    } verify { result ->
        assertTrue(result.output.contains("Could not resolve"), result.output)
    }

    @Test
    fun `fingerprint handles classpath entries that are directories and changes when directory contents change`() = setup(object {
        val classpathDirPath = "build/classes/kotlin/js/main"
        val markerFileName = "marker.txt"
        val initialMarker = "v1"
        val updatedMarker = "v2"
        val marker = fileUnderProject("$classpathDirPath/$markerFileName")
    }) {
        writeSettings("classpath-directory-entry-test")

        val classpathDir = fileUnderProject(classpathDirPath).apply { mkdirs() }
        marker.writeText(initialMarker)

        writeBuild(
            """
            plugins {
                id("com.zegreatrob.tools.fingerprint")
            }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("1.0")
                classpath.setFrom(files("${classpathDir.invariantSeparatorsPath}"))
            }
            """.trimIndent(),
        )
    } exercise {
        val baseline = runFingerprintWithManifest()

        marker.writeText(updatedMarker)

        val afterChange = runFingerprintWithManifest()

        Pair(baseline, afterChange)
    } verify { (baseline, afterChange) ->
        assertManifestContainsDependencyIngredients(baseline.manifest, context = "baseline (directory classpath entry)")
        assertManifestContainsDependencyIngredients(afterChange.manifest, context = "after directory content change")

        assertFingerprintChanged(
            baseline.hash,
            afterChange.hash,
            "Fingerprint should change when a directory classpath entry contents change!",
        )
        assertTrue(
            baseline.manifest != afterChange.manifest,
            "Manifest should change when a directory classpath entry contents change.\n--- first ---\n${baseline.manifest}\n--- second ---\n${afterChange.manifest}",
        )
    }

    private data class CacheRunResult(
        val result: BuildResult,
        val firstManifest: String,
        val secondManifest: String,
    )
}
