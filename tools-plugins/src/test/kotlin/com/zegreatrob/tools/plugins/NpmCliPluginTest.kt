package com.zegreatrob.tools.plugins

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class NpmCliPluginTest {
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
