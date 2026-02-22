package com.zegreatrob.tools.digger

import org.gradle.testkit.runner.GradleRunner
import java.io.File

class CurrentContributionFunctionalTest : CurrentContributionTestSpec {
    override lateinit var projectDir: String

    private val buildFile by lazy { "$projectDir/build.gradle.kts" }
    private val settingsFile by lazy { "$projectDir/settings.gradle" }
    private val ignoreFile by lazy { "$projectDir/.gitignore" }

    override val addFileNames by lazy {
        setOf(
            settingsFile.split("/").last(),
            buildFile.split("/").last(),
            ignoreFile.split("/").last(),
        )
    }

    private fun setup() {
        File(settingsFile).writeText("includeBuild(\"\${System.getProperty(\"user.dir\")}/../../tools\")")
        File(ignoreFile).writeText(".gradle")
    }

    override fun setupWithDefaults() {
        setup()
        File(buildFile).writeText(
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
        setup()
        File(buildFile).writeText(
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
                ${if (easeRegex != null) """easeRegex.set(Regex("${easeRegex.replace("\\", "\\\\")}"))""" else ""}
                ${if (tagRegex != null) """tagRegex.set(Regex("${tagRegex.replace("\\", "\\\\")}"))""" else ""}
            }
            """.trimIndent(),
        )
    }

    override fun runCurrentContributionData(): String {
        val currentOutput by lazy { "$projectDir/build/digger/current.json" }
        GradleRunner.create()
            .forwardOutput()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(File(projectDir))
            .build()
        return File(currentOutput).readText()
    }
}
