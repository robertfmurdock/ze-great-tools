package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class AdditionalTasksFunctionalTest {
    @Test
    fun previousVersionOutputsLastTag() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val initialTag = "1.2.3"
    }) {
        writeSettings(projectDir)
        writeBuildFile(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = initialTag,
            commits = listOf("init"),
        )
    } exercise {
        runGradle(projectDir, "previousVersion")
    } verifyAnd { output ->
        output.trim().assertIsEqualTo(initialTag)
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun commitReportOutputsReport() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
    }) {
        writeSettings(projectDir)
        writeBuildFile(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        runGradle(projectDir, "commitReport")
    } verifyAnd { output ->
        output.contains("COMMIT REPORT-------").assertIsEqualTo(true)
        output.contains("COMMIT REPORT OVAH--").assertIsEqualTo(true)
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun releaseDryRunAvoidsImplicitGitHeadDependency() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
    }) {
        writeSettings(projectDir)
        writeBuildFileWithDigger(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        runGradle(
            projectDir = projectDir,
            task = "release",
            quiet = false,
            extraArgs = listOf("--dry-run", "--warning-mode=all", "--console=plain"),
        )
    } verifyAnd { output ->
        output.contains("implicit dependency")
            .assertIsEqualTo(false, "Unexpected implicit dependency warning.\n$output")
        output.contains("validation_problems")
            .assertIsEqualTo(false, "Unexpected validation problems output.\n$output")
    } teardown {
        removeDirectory(projectDir)
    }

    private fun writeSettings(projectDir: String) {
        File("$projectDir/settings.gradle").writeText(
            "includeBuild(\"${System.getProperty("user.dir")}/../../tools\")",
        )
    }

    private fun writeBuildFile(projectDir: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent(),
        )
    }

    private fun writeBuildFileWithDigger(projectDir: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
                id("com.zegreatrob.tools.digger")
            }
            version = "1.2.3"
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent(),
        )
    }

    private fun writeGitIgnore(projectDir: String) {
        File("$projectDir/.gitignore").writeText(".gradle")
    }

    private fun runGradle(
        projectDir: String,
        task: String,
        quiet: Boolean = true,
        extraArgs: List<String> = emptyList(),
    ): String {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        val args = mutableListOf(task)
        if (quiet) {
            args.add("-q")
        }
        args.addAll(extraArgs)
        runner.withArguments(args)
        return runner.build().output.trim()
    }
}
