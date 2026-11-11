package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSuiteConventionTest {
    @Test
    fun configuresSharedTestDependenciesForMultiplatformProjects() = setup(object {
        val directory = Files.createTempDirectory("test-suite-convention").toFile()

        init {
            directory.resolve("settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories { mavenCentral() }
                }
                """.trimIndent(),
            )
            directory.resolve("gradle").mkdirs()
            directory.resolve("gradle/libs.versions.toml").writeText(
                """
                [libraries]
                com-zegreatrob-testmints-bom = "com.zegreatrob.testmints:testmints-bom:12.6.70"
                """.trimIndent(),
            )
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.zegreatrob.tools.plugins.test-suite")
                    id("org.jetbrains.kotlin.multiplatform")
                }
                kotlin { jvm() }
                """.trimIndent(),
            )
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("tasks", "--all", "dependencies", "--configuration", "jvmCompileClasspath", "--console=plain")
            .build()
    } verifyAnd { result ->
        assertTrue(result.output.contains("com.zegreatrob.testmints:testmints-bom:12.6.70"))
        assertTrue(result.output.contains("collectResults - Collects reports and test results"))
        assertTrue(result.output.contains("lintKotlin"))
    } teardown {
        directory.deleteRecursively()
    }
}
