package com.zegreatrob.tools.digger

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DiggerPluginFunctionalTest :
    CurrentContributionTestSpec,
    AllContributionTestSpec {
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

    override fun setupWithOverrides(label: String?) {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            digger {
                ${if (label != null) "label.set(\"$label\")" else ""}
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

    override fun runCurrentContributionData(): String {
        val currentOutput by lazy { projectDir.resolve("build/digger/current.json") }
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        return currentOutput.readText()
    }
}
