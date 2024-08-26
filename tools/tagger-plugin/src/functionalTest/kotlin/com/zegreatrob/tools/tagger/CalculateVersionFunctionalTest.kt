package com.zegreatrob.tools.tagger

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest

class CalculateVersionFunctionalTest : CalculateVersionTestSpec {
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

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
    ) {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                ${if (implicitPatch != null) "implicitPatch.set($implicitPatch)" else ""}
                ${if (majorRegex != null) "majorRegex.set(Regex(\"${majorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (minorRegex != null) "minorRegex.set(Regex(\"${minorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (patchRegex != null) "patchRegex.set(Regex(\"${patchRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (noneRegex != null) "noneRegex.set(Regex(\"${noneRegex.replace("\\", "\\\\")}\"))" else ""}
                ${
                if (versionRegex != null) {
                    "versionRegex.set(Regex(\"${
                        versionRegex.replace(
                            "\\",
                            "\\\\",
                        )
                    }\"))"
                } else {
                    ""
                }
            }
            }
            """.trimIndent(),
        )
    }

    override fun execute(): TestResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        return try {
            val result = runner.build()
            result.output.trim().let(TestResult::Success)
        } catch (e: Exception) {
            TestResult.Failure(e.message!!)
        }
    }
}
