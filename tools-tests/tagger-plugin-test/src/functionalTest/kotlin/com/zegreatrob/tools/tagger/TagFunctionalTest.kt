package com.zegreatrob.tools.tagger

import org.gradle.testkit.runner.GradleRunner
import java.io.File

class TagFunctionalTest : TagTestSpec {

    override lateinit var projectDir: String

    private val buildFile by lazy { "$projectDir/build.gradle.kts" }
    private val settingsFile by lazy { "$projectDir/settings.gradle" }
    private val ignoreFile by lazy { "$projectDir/.gitignore" }
    override val addFileNames: Set<String>
        get() = setOf(buildFile, settingsFile, ignoreFile).map { it.split("/").last() }.toSet()

    private fun setup() {
        File(settingsFile).writeText("includeBuild(\"\${System.getProperty(\"user.dir\")}/../../tools\")")
        File(ignoreFile).writeText(".gradle")
    }

    override fun configureWithDefaults() {
        setup()
        File(buildFile).writeText(
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

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        setup()
        File(buildFile).writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                ${if (releaseBranch != null) "releaseBranch = \"$releaseBranch\"" else ""}
                ${if (userName != null) "userName = \"$userName\"" else ""}
                ${if (userEmail != null) "userEmail = \"$userEmail\"" else ""}
                ${if (warningsAsErrors != null) "warningsAsErrors.set($warningsAsErrors)" else ""}
            }
            """.trimIndent(),
        )
    }

    override fun execute(version: String): TestResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withArguments("tag", "-Pversion=$version")
        runner.withProjectDir(File(projectDir))
        return try {
            val result = runner.build()
            result.output.trim().let(TestResult::Success)
        } catch (e: Exception) {
            TestResult.Failure(e.message!!.trim())
        }
    }
}
