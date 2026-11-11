package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpmCliPluginTest {
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
        val directory = Files.createTempDirectory("npm-cli-publish").toFile()
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
    } verifyAnd { result ->
        assertTrue(result.output.contains("Reusing configuration cache."))
        assertEquals(expectedArguments, output.readLines())
    } teardown {
        directory.deleteRecursively()
    }

    @Test
    fun cleaningReadmePreservesLicense() = setup(object {
        val directory = Files.createTempDirectory("npm-cli-plugin-test").toFile()

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
        }
    }) exercise {
        GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withArguments("cleanCopyReadme")
            .build()
    } verifyAnd {
        assertTrue(directory.resolve("build/js/packages/fixture/LICENSE").isFile)
    } teardown {
        directory.deleteRecursively()
    }
}
