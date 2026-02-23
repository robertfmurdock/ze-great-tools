package com.zegreatrob.tools.fingerprint

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

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
        val includedDir = projectDir(includedBuildName)
        val mainDir = projectDir(mainBuildName)
    }) {
        writeSettings(includedDir, includedBuildName)

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

        writeSettings(mainDir, mainBuildName, includeBuildName = includedBuildName)

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

        (aggregateHash != localHash).assertIsEqualTo(true, "Aggregate should not match local hash")
        (aggregateHash != includedHash).assertIsEqualTo(true, "Aggregate should not match included hash")
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`() = setup(object {
        val includedBuildName = "my-lib"
        val mainBuildName = "my-app"
        val mainDir = projectDir(mainBuildName)
    }) {
        val includedDir = projectDir(includedBuildName)
        writeSettings(includedDir, includedBuildName)
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        writeSettings(mainDir, mainBuildName, includeBuildName = includedBuildName)
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
        includedTaskResult.assertIsNotEqualTo(null, "Task in included build should have been triggered")
        (includedTaskResult?.outcome == TaskOutcome.SUCCESS || includedTaskResult?.outcome == TaskOutcome.UP_TO_DATE)
            .assertIsEqualTo(true, "Included task should have executed successfully")
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`() = setup(object {
        val includedBuildName = "naked-lib"
        val mainBuildName = "main-app"
        val mainDir = projectDir(mainBuildName)
    }) {
        val nakedDir = projectDir(includedBuildName)
        writeSettings(nakedDir, includedBuildName)
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        writeSettings(mainDir, mainBuildName, includeBuildName = includedBuildName)
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")
    } verify { result ->
        result.output.contains("SUCCESS")
            .assertIsEqualTo(true, "Build should succeed even with non-plugin included builds")
        val aggregateFile = mainDir.resolve("build/aggregate-fingerprint.txt")
        aggregateFile.exists().assertIsEqualTo(true, "Aggregate file should still be generated")
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
        val includedDir = projectDir(includedBuildName)
        val mainDir = projectDir(mainBuildName)
    }) {
        writeSettings(includedDir, includedBuildName)
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

        writeSettings(mainDir, mainBuildName, includeBuildName = includedBuildName)
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
        aggregateFingerprintFile(mainDir).exists().assertIsEqualTo(
            true,
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(mainDir).path}",
        )

        val aggregateManifest = aggregateFingerprintManifestFile(mainDir)
        aggregateManifest.exists().assertIsEqualTo(
            true,
            "Aggregate manifest log should be generated at ${aggregateManifest.path}",
        )

        val text = aggregateManifest.readText()

        text.contains(includedMarkerName).assertIsEqualTo(
            true,
            "Aggregate manifest should include evidence from the included build manifest.\n--- aggregate ---\n$text",
        )
        text.contains(mainMarkerName).assertIsEqualTo(
            true,
            "Aggregate manifest should include evidence from the local/main manifest.\n--- aggregate ---\n$text",
        )

        val pluginVersionLines = text.lineSequence().filter { it.startsWith("pluginVersion|") }.toList()
        (pluginVersionLines.size >= 2).assertIsEqualTo(
            true,
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
        result.task(":aggregateFingerprints")?.outcome.assertIsEqualTo(TaskOutcome.SUCCESS)

        fingerprintFile().exists()
            .assertIsEqualTo(true, "Local fingerprint should be generated at ${fingerprintFile().path}")
        assertFingerprintManifestGeneratedCorrectly()

        aggregateFingerprintFile(testProjectDir).exists().assertIsEqualTo(
            true,
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(testProjectDir).path}",
        )
        aggregateFingerprintManifestFile(testProjectDir).exists().assertIsEqualTo(
            true,
            "Aggregate manifest log should be generated at ${aggregateFingerprintManifestFile(testProjectDir).path}",
        )
    }
}
