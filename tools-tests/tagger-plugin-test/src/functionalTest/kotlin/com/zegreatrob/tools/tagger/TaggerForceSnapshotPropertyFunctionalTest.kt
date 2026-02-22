package com.zegreatrob.tools.tagger

import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class TaggerForceSnapshotPropertyFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `can force snapshot via -PtaggerForceSnapshot`() = setup(object {
        val buildFile = projectDir.resolve("build.gradle.kts")
        val settingsFile = projectDir.resolve("settings.gradle")
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(
                "calculateVersion",
                "-q",
                "-PtaggerForceSnapshot=true",
            )
    }) {
        settingsFile.writeText("includeBuild(\"\${System.getProperty(\"user.dir\")}/../../tools\")")
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
            addFileNames = setOf(buildFile.name, settingsFile.name),
            initialTag = "1.2.3",
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        runner.build().output
    } verify { output ->
        assertContains(output, "1.2.4-SNAPSHOT")
        assertContains(output, "FORCED")
    }
}
