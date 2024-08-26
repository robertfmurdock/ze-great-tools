package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.runProcess
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest

class TagFunctionalTest : TagTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }
    override val addFileNames: Set<String>
        get() = setOf(buildFile, settingsFile, ignoreFile).map { it.name }.toSet()

    @BeforeTest
    fun setup() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
    }

    override fun configureWithDefaults() {
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
    }

    override fun execute(version: String): TestResult {
        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), this.projectDir.absolutePath)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), this.projectDir.absolutePath)

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("tag", "-Pversion=$version")
        runner.withProjectDir(projectDir)
        return try {
            val result = runner.build()
            result.output.trim().let(TestResult::Success)
        } catch (e: Exception) {
            TestResult.Failure(e.message!!)
        }
    }
}
