package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportsPluginTest {
    @Test
    fun reportsConventionProvidesItsCheckTaskPrerequisite() = setup(object {
        val directory = Files.createTempDirectory("reports-plugin-prerequisite").toFile()

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
    } verifyAnd { result ->
        assertTrue(result.output.contains("Collects reports and test results"))
    } teardown {
        directory.deleteRecursively()
    }

    @Test
    fun collectionPreservesFilesWithMatchingRelativeNames() = setup(object {
        val directory = Files.createTempDirectory("reports-plugin-collision").toFile()

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
    } verifyAnd {
        assertEquals("report", directory.resolve("build/test-output/results/reports/jvmTest/shared.xml").readText())
        assertEquals("result", directory.resolve("build/test-output/results/test-results/jvmTest/shared.xml").readText())
    } teardown {
        directory.deleteRecursively()
    }

    @Test
    fun helpDoesNotCreateReportOutputDirectory() = setup(object {
        val directory = Files.createTempDirectory("reports-plugin-help").toFile()

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
    } verifyAnd {
        assertFalse(directory.resolve("build/test-output").exists())
    } teardown {
        directory.deleteRecursively()
    }

    @Test
    fun cleaningCopiedReportsPreservesCopiedTestResults() = setup(object {
        val directory = Files.createTempDirectory("reports-plugin-test").toFile()

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
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("cleanCopyReportsToRootDirectory")
            .build()
    } verifyAnd {
        assertTrue(directory.resolve("build/test-output/results/test-results/TEST.xml").isFile)
    } teardown {
        directory.deleteRecursively()
    }
}
