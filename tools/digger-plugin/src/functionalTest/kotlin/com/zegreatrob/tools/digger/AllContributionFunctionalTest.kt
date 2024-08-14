package com.zegreatrob.tools.digger

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AllContributionFunctionalTest : AllContributionTestSpec {
    @field:TempDir
    override lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val allOutput by lazy { projectDir.resolve("build/digger/all.json") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }

    override val addFileNames by lazy { setOf(settingsFile.name, buildFile.name, ignoreFile.name) }

    override fun setupWithDefaults() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )
    }

    override fun setupWithOverrides(
        label: String?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        noneRegex: String?,
        storyRegex: String?,
        easeRegex: String?,
        tagRegex: String?,
    ) {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            digger {
                ${if (label != null) "label.set(\"$label\")" else ""}
                ${if (majorRegex != null) "majorRegex.set(Regex(\"${majorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (minorRegex != null) "minorRegex.set(Regex(\"${minorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (patchRegex != null) "patchRegex.set(Regex(\"${patchRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (noneRegex != null) "noneRegex.set(Regex(\"${noneRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (storyRegex != null) "storyIdRegex.set(Regex(\"${storyRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (easeRegex != null) "easeRegex.set(Regex(\"${easeRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (tagRegex != null) "tagRegex.set(Regex(\"${tagRegex.replace("\\", "\\\\")}\"))" else ""}
            }
            """.trimIndent(),
        )
    }

    override fun runAllContributionData(): String {
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        return allOutput.readText()
    }
}
