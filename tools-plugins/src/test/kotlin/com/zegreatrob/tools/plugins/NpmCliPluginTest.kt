package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpmCliPluginTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun npmTasksCanBeConfiguredBeforeKotlinTargetsAreDeclared() = setup(object {
        init {
            directory.resolve("settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                dependencyResolutionManagement { repositories { mavenCentral() } }
                """.trimIndent(),
            )
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.zegreatrob.tools.plugins.npm-cli")
                    id("org.jetbrains.kotlin.multiplatform")
                }
                npmCli {
                    packageName.set("@continuous-excellence/fixture")
                    description.set("Fixture CLI")
                }
                tasks.named("copyReadme") {
                    description = "Configured during project evaluation"
                }
                kotlin {
                    js { nodejs() }
                }
                """.trimIndent(),
            )
        }
        val build = GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("jsPackageJson", "--configuration-cache")
    }) exercise {
        build.build()
        build.build()
    } verify { result ->
        assertTrue(result.output.contains("Reusing configuration cache."))
        val packageJson = JsonSlurper().parse(directory.resolve("build/js/packages/fixture/package.json")) as Map<*, *>
        assertEquals("@continuous-excellence/fixture", packageJson["name"])
        assertEquals("Fixture CLI", packageJson["description"])
    }

    @Test
    fun snapshotPublishReusesConfigurationCache() = publishReusesConfigurationCache(
        "1.2.3-SNAPSHOT",
        listOf("publish", "--dry-run", "--access", "public", "--tag", "snapshot"),
    )

    @Test
    fun releasePublishReusesConfigurationCache() = publishReusesConfigurationCache(
        "1.2.3",
        listOf("publish", "--access", "public", "--provenance"),
    )

    private fun publishReusesConfigurationCache(version: String, expectedArguments: List<String>) = setup(object {
        val bin = directory.resolve("bin").apply { mkdirs() }
        val output = directory.resolve("npm-args.txt")

        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("org.jetbrains.kotlin.multiplatform")
                    id("com.zegreatrob.tools.plugins.npm-cli")
                }
                version = "$version"
                kotlin {
                    js {
                        nodejs()
                        binaries.executable()
                    }
                }
                afterEvaluate {
                    tasks.named<Exec>("jsPublish") {
                        actions.add(1, Action { (this as Exec).executable = "${bin.resolve("npm").absolutePath}" })
                    }
                }
                """.trimIndent(),
            )
            directory.resolve("build/js/packages/fixture").mkdirs()
            bin.resolve("npm").apply {
                writeText("#!/bin/sh\nprintf '%s\\n' \"\$@\" > '${output.absolutePath}'\n")
                setExecutable(true)
            }
        }
        val build = GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("jsPublish", "-x", "jsCliTar", "--configuration-cache")
    }) exercise {
        build.build()
        build.build()
    } verify { result ->
        assertTrue(result.output.contains("Reusing configuration cache."))
        assertEquals(expectedArguments, output.readLines())
    }

    @Test
    fun cleaningReadmeRemovesReadmeAndPreservesLicense() = setup(object {
        val cleanedCopy = directory.resolve("build/js/packages/fixture/README.md")
        val preservedCopy = directory.resolve("build/js/packages/fixture/LICENSE")

        init {
            directory.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
            directory.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("org.jetbrains.kotlin.multiplatform")
                    id("com.zegreatrob.tools.plugins.npm-cli")
                }
                kotlin {
                    js {
                        nodejs()
                        binaries.executable()
                    }
                }
                """.trimIndent(),
            )
            directory.resolve("README.md").writeText("readme")
            directory.resolve("LICENSE").writeText("license")
            GradleRunner.create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments("copyReadme", "copyLicense", "-x", "jsPackageJson", "-x", "kotlinNpmInstall")
                .build()
            check(cleanedCopy.isFile) { "Expected a copied file at $cleanedCopy" }
            check(preservedCopy.readText() == "license") { "Expected original contents at $preservedCopy" }
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("cleanCopyReadme")
            .build()
    } verify {
        assertFalse(cleanedCopy.exists())
        assertEquals("license", preservedCopy.readText())
    }
}
