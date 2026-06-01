package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class TaggerDSLDisableDetachedDeprecationTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `when disableDetached is NOT configured do NOT emit deprecation warning`() = setup(object {
        val buildFile = projectDir.resolve("build.gradle.kts")
        val settingsFile = projectDir.resolve("settings.gradle")
        val ignoreFile = projectDir.resolve(".gitignore")
    }) {
        settingsFile.writeText("""includeBuild("${System.getProperty("user.dir")}/../../tools")""")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent(),
        )

        initializeGitRepo(
            directory = projectDir.absolutePath,
            remoteUrl = projectDir.absolutePath,
            addFileNames = setOf(buildFile.name, settingsFile.name, ignoreFile.name),
            initialTag = "1.2.3",
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("calculateVersion", "-q")
        val result = runner.build()
        result.output
    } verify { output ->
        output.contains("disableDetached").assertIsEqualTo(false, "Should NOT mention disableDetached. Output:\n$output")
        output.contains("deprecated").assertIsEqualTo(false, "Should NOT show deprecation warning. Output:\n$output")
    }

    @Test
    fun `when disableDetached is set via DSL emit deprecation warning`() = setup(object {
        val buildFile = projectDir.resolve("build.gradle.kts")
        val settingsFile = projectDir.resolve("settings.gradle")
        val ignoreFile = projectDir.resolve(".gitignore")
    }) {
        settingsFile.writeText("""includeBuild("${System.getProperty("user.dir")}/../../tools")""")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                disableDetached.set(false)
            }
            """.trimIndent(),
        )

        initializeGitRepo(
            directory = projectDir.absolutePath,
            remoteUrl = projectDir.absolutePath,
            addFileNames = setOf(buildFile.name, settingsFile.name, ignoreFile.name),
            initialTag = "1.2.3",
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("calculateVersion", "-q")
        val result = runner.build()
        result.output
    } verify { output ->
        output.contains("disableDetached").assertIsEqualTo(true, "Expected disableDetached in output. Output:\n$output")
        output.contains("deprecated").assertIsEqualTo(true, "Expected deprecation warning. Output:\n$output")
        output.contains("allowDetachedHead").assertIsEqualTo(true, "Expected migration guidance. Output:\n$output")
    }
}
