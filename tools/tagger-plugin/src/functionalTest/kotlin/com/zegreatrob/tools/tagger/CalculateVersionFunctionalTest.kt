package com.zegreatrob.tools.tagger

import org.gradle.testkit.runner.GradleRunner
import java.io.File

class CalculateVersionFunctionalTest : CalculateVersionTestSpec {
    override lateinit var projectDir: String

    private val buildFile by lazy { "$projectDir/${"build.gradle.kts"}" }
    private val settingsFile by lazy { "$projectDir/${"settings.gradle"}" }
    private val ignoreFile by lazy { "$projectDir/${".gitignore"}" }
    override val addFileNames: Set<String>
        get() = setOf(buildFile, settingsFile, ignoreFile).map { it.split("/").last() }.toSet()

    private fun setup() {
        File(settingsFile).writeText("")
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
        implicitPatch: Boolean?,
        disableDetached: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
    ) {
        setup()
        File(buildFile).writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                ${if (implicitPatch != null) "implicitPatch.set($implicitPatch)" else ""}
                ${if (disableDetached != null) "disableDetached.set($disableDetached)" else ""}
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
                ${if (forceSnapshot != null) "forceSnapshot.set($forceSnapshot)" else ""}
            }
            """.trimIndent(),
        )
    }

    override fun execute(): TestResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(File(projectDir))
        return try {
            val result = runner.build()
            val lines = result.output
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()

            val version = lines.firstOrNull().orEmpty()
            val details = lines.drop(1).joinToString("\n")

            TestResult.Success(version, details)
        } catch (e: Exception) {
            TestResult.Failure(e.message!!)
        }
    }
}
