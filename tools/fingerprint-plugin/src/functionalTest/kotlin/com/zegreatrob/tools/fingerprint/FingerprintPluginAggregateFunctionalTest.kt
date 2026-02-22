package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FingerprintPluginAggregateFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `aggregateFingerprints combines hashes from included builds`() = setup(object {
        lateinit var includedDir: File
        lateinit var mainDir: File
    }) {
        includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-included-lib"""")
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

        mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
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
    } exercise {
        gradle(mainDir, ":my-included-lib:generateFingerprint", "aggregateFingerprints")
    } verify {
        val localHash = fingerprintFile(mainDir).readText()
        val includedHash = fingerprintFile(includedDir).readText()
        val aggregateHash = aggregateFingerprintFile(mainDir).readText()

        assert(aggregateHash != localHash) { "Aggregate should not match local hash" }
        assert(aggregateHash != includedHash) { "Aggregate should not match included hash" }
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`() = setup(object {
        lateinit var mainDir: File
    }) {
        val includedDir = testProjectDir.resolve("my-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-lib"""")
        includedDir.resolve("build.gradle.kts").writeText("""plugins { id("com.zegreatrob.tools.fingerprint") }""")

        mainDir = testProjectDir.resolve("my-app").apply { mkdirs() }
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
    } exercise {
        gradle(mainDir, "aggregateFingerprints")
    } verify { result ->
        val includedTaskResult = result.task(":my-lib:generateFingerprint")
        assertNotNull(includedTaskResult, "Task in included build should have been triggered")
        assertTrue(
            includedTaskResult.outcome == TaskOutcome.SUCCESS || includedTaskResult.outcome == TaskOutcome.UP_TO_DATE,
            "Included task should have executed successfully",
        )
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`() = setup(object {
        lateinit var mainDir: File
    }) {
        val nakedDir = testProjectDir.resolve("naked-lib").apply { mkdirs() }
        nakedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "naked-lib"""")
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "main-app"
            includeBuild("../naked-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText("""plugins { id("com.zegreatrob.tools.fingerprint") }""")
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")
    } verify { result ->
        assertTrue(result.output.contains("SUCCESS"), "Build should succeed even with non-plugin included builds")
        assertTrue(aggregateFingerprintFile(mainDir).exists(), "Aggregate file should still be generated")
    }

    @Test
    fun `aggregateFingerprints works when includedBuilds is not configured`() = setup(object {}) {
        val projectName = "aggregate-defaults-test"
        val buildScript = """plugins { id("com.zegreatrob.tools.fingerprint") }"""

        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))
    } verify { result ->
        assertEquals(TaskOutcome.SUCCESS, result.task(":aggregateFingerprints")?.outcome)
        assertTrue(fingerprintFile().exists(), "Local fingerprint should be generated at ${fingerprintFile().path}")
        assertFingerprintManifestGeneratedCorrectly()
        assertTrue(aggregateFingerprintFile(testProjectDir).exists())
        assertTrue(aggregateFingerprintManifestFile(testProjectDir).exists())
    }
}
