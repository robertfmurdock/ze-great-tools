package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportsPluginTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun reportsConventionRegistersTheCollectionTask() = setup(object {
        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """plugins { id("com.zegreatrob.tools.plugins.reports") }""",
            )
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("help", "--task", "collectResults")
            .build()
    } verify { result ->
        assertTrue(result.output.contains("Collects reports and test results"))
    }

    @Test
    fun collectionPreservesFilesWithMatchingRelativeNames() = setup(object {
        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    base
                    id("com.zegreatrob.tools.plugins.reports")
                }
                """.trimIndent(),
            )
            directory.resolve("build/reports/jvmTest").mkdirs()
            directory.resolve("build/reports/jvmTest/shared.xml").writeText("report")
            directory.resolve("build/test-results/jvmTest").mkdirs()
            directory.resolve("build/test-results/jvmTest/shared.xml").writeText("result")
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("collectResults")
            .build()
    } verify {
        assertEquals("report", directory.resolve("build/test-output/results/reports/jvmTest/shared.xml").readText())
        assertEquals("result", directory.resolve("build/test-output/results/test-results/jvmTest/shared.xml").readText())
    }

    @Test
    fun helpDoesNotCreateReportOutputDirectory() = setup(object {
        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    base
                    id("com.zegreatrob.tools.plugins.reports")
                }
                """.trimIndent(),
            )
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("help")
            .build()
    } verify {
        assertFalse(directory.resolve("build/test-output").exists())
    }

    @Test
    fun cleaningCopiedReportsRemovesReportsAndPreservesTestResults() = setup(object {
        val cleanedCopy = directory.resolve("build/test-output/results/reports/report.txt")
        val preservedCopy = directory.resolve("build/test-output/results/test-results/TEST.xml")

        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    base
                    id("com.zegreatrob.tools.plugins.reports")
                }
                """.trimIndent(),
            )
            directory.resolve("build/reports").mkdirs()
            directory.resolve("build/reports/report.txt").writeText("report")
            directory.resolve("build/test-results").mkdirs()
            directory.resolve("build/test-results/TEST.xml").writeText("results")
            GradleRunner.create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments("collectResults")
                .build()
            check(cleanedCopy.isFile) { "Expected a copied file at $cleanedCopy" }
            check(preservedCopy.readText() == "results") { "Expected original contents at $preservedCopy" }
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("cleanCopyReportsToRootDirectory")
            .build()
    } verify {
        assertFalse(cleanedCopy.exists())
        assertEquals("results", preservedCopy.readText())
    }
}
