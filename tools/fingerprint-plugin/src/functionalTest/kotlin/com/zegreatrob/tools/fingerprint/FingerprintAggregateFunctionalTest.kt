package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FingerprintAggregateFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `aggregateFingerprints combines hashes from included builds`() = setup(object {
        val includedBuildName = "my-included-lib"
        val mainBuildName = "main-app"
        val includedMarkerName = "included-marker.txt"
        val includedMarkerContent = "lib-content"
        val mainMarkerName = "main-marker.txt"
        val mainMarkerContent = "app-content"
        val pluginVersion = "1.0"
        lateinit var includedDir: File
        lateinit var mainDir: File
    }) {
        includedDir = testProjectDir.resolve(includedBuildName).apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "$includedBuildName"""")

        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            val includedMarker = file("$includedMarkerName").apply { writeText("$includedMarkerContent") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("$pluginVersion")
                classpath.setFrom(files(includedMarker))
            }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve(mainBuildName).apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "$mainBuildName"
            includeBuild("../$includedBuildName")
            """.trimIndent(),
        )

        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("multiplatform") version "2.3.0" apply false }

            val mainMarker = file("$mainMarkerName").apply { writeText("$mainMarkerContent") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("$pluginVersion")
                classpath.setFrom(files(mainMarker))
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, ":$includedBuildName:generateFingerprint", "aggregateFingerprints")
    } verify {
        val localHash = fingerprintFile(mainDir).readText()
        val includedHash = fingerprintFile(includedDir).readText()
        val aggregateHash = mainDir.resolve("build/aggregate-fingerprint.txt").readText()

        assert(aggregateHash != localHash) { "Aggregate should not match local hash" }
        assert(aggregateHash != includedHash) { "Aggregate should not match included hash" }
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`() = setup(object {
        val includedBuildName = "my-lib"
        val mainBuildName = "my-app"
        lateinit var mainDir: File
    }) {
        val includedDir = testProjectDir.resolve(includedBuildName).apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "$includedBuildName"""")
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve(mainBuildName).apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "$mainBuildName"
            includeBuild("../$includedBuildName")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            fingerprintConfig {
                includedBuilds.add("$includedBuildName")
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints")
    } verify { result ->
        val includedTaskResult = result.task(":$includedBuildName:generateFingerprint")
        assertNotNull(includedTaskResult, "Task in included build should have been triggered")
        assertTrue(
            includedTaskResult.outcome == TaskOutcome.SUCCESS || includedTaskResult.outcome == TaskOutcome.UP_TO_DATE,
            "Included task should have executed successfully",
        )
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`() = setup(object {
        val includedBuildName = "naked-lib"
        val mainBuildName = "main-app"
        lateinit var mainDir: File
    }) {
        val nakedDir = testProjectDir.resolve(includedBuildName).apply { mkdirs() }
        nakedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "$includedBuildName"""")
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        mainDir = testProjectDir.resolve(mainBuildName).apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "$mainBuildName"
            includeBuild("../$includedBuildName")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")
    } verify { result ->
        assertTrue(result.output.contains("SUCCESS"), "Build should succeed even with non-plugin included builds")
        val aggregateFile = mainDir.resolve("build/aggregate-fingerprint.txt")
        assertTrue(aggregateFile.exists(), "Aggregate file should still be generated")
    }

    @Test
    fun `aggregateFingerprints merges included + local manifest logs into one aggregate manifest log file`() = setup(object {
        val includedBuildName = "my-included-lib"
        val mainBuildName = "main-app"
        val includedMarkerName = "included-marker.txt"
        val includedMarkerContent = "included-content"
        val mainMarkerName = "main-marker.txt"
        val mainMarkerContent = "main-content"
        val includedPluginVersion = "included-1.0"
        val mainPluginVersion = "main-1.0"
        lateinit var includedDir: File
        lateinit var mainDir: File
    }) {
        includedDir = testProjectDir.resolve(includedBuildName).apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "$includedBuildName"""")
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            val includedMarker = file("$includedMarkerName").apply { writeText("$includedMarkerContent") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("$includedPluginVersion")
                classpath.setFrom(files(includedMarker))
            }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve(mainBuildName).apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "$mainBuildName"
            includeBuild("../$includedBuildName")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                includedBuilds.add("$includedBuildName")
            }

            val mainMarker = file("$mainMarkerName").apply { writeText("$mainMarkerContent") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("$mainPluginVersion")
                classpath.setFrom(files(mainMarker))
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--no-configuration-cache")
    } verify {
        assertTrue(
            aggregateFingerprintFile(mainDir).exists(),
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(mainDir).path}",
        )

        val aggregateManifest = aggregateFingerprintManifestFile(mainDir)
        assertTrue(
            aggregateManifest.exists(),
            "Aggregate manifest log should be generated at ${aggregateManifest.path}",
        )

        val text = aggregateManifest.readText()

        assertTrue(
            text.contains(includedMarkerName),
            "Aggregate manifest should include evidence from the included build manifest.\n--- aggregate ---\n$text",
        )
        assertTrue(
            text.contains(mainMarkerName),
            "Aggregate manifest should include evidence from the local/main manifest.\n--- aggregate ---\n$text",
        )

        val pluginVersionLines = text.lineSequence().filter { it.startsWith("pluginVersion|") }.toList()
        assertTrue(
            pluginVersionLines.size >= 2,
            "Aggregate manifest should contain pluginVersion lines from multiple manifests. Found=${pluginVersionLines.size}\n--- aggregate ---\n$text",
        )
    }

    @Test
    fun `aggregateFingerprints works when includedBuilds is not configured`() = setup(object {
        val projectName = "aggregate-defaults-test"
        val buildScript = """
            plugins { id("com.zegreatrob.tools.fingerprint") }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))
    } verify { result ->
        assertEquals(TaskOutcome.SUCCESS, result.task(":aggregateFingerprints")?.outcome)

        assertTrue(fingerprintFile().exists(), "Local fingerprint should be generated at ${fingerprintFile().path}")
        assertFingerprintManifestGeneratedCorrectly()

        assertTrue(
            aggregateFingerprintFile(testProjectDir).exists(),
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(testProjectDir).path}",
        )
        assertTrue(
            aggregateFingerprintManifestFile(testProjectDir).exists(),
            "Aggregate manifest log should be generated at ${aggregateFingerprintManifestFile(testProjectDir).path}",
        )
    }
}
