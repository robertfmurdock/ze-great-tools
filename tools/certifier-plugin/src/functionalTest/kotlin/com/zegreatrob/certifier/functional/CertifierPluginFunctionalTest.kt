package com.zegreatrob.certifier.functional

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class CertifierPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }

    private fun certificatePath() = javaClass.getResource("/localhost.crt")?.toURI()?.path
        ?: error("Test not setup correctly - missing /localhost.crt")

    private fun initializeProjectFiles() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
    }

    private fun writeBuildFile(certificatePath: String? = certificatePath(), jdkSelector: String? = "21") {
        val installCertConfig = buildString {
            if (jdkSelector != null) appendLine("                    jdkSelector = \"$jdkSelector\"")
            if (certificatePath != null) appendLine("                    certificatePath = \"$certificatePath\"")
        }.trimEnd()
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.certifier")
            }

            tasks {
                installCert {
$installCertConfig
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun installCertIsConfigurationCacheCompatible() = setup(object {
        val args = listOf("installCert", "--configuration-cache", "-m")
    }) {
        initializeProjectFiles()
        writeBuildFile()
    } exercise {
        val firstRun = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir)
            .build()
        val secondRun = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir)
            .build()
        firstRun to secondRun
    } verify { (firstRun, secondRun) ->
        firstRun.output.contains("Configuration cache entry stored").assertIsEqualTo(true, firstRun.output)
        secondRun.output.contains("Reusing configuration cache.").assertIsEqualTo(true, secondRun.output)
    }

    @Test
    fun canRunInstallCertTask() = setup(object {
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        initializeProjectFiles()
        writeBuildFile()
    } exercise {
        runner.build()
    } verify { result ->
        result.task(":installCert")?.outcome.assertIsEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun willEmitErrorWhenNoJdkSelected() = setup(object {
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        initializeProjectFiles()
        writeBuildFile(jdkSelector = null)
    } exercise {
        runner.buildAndFail()
    } verify { result ->
        result.output.contains("property 'jdkSelector' doesn't have a configured value")
            .assertIsEqualTo(true, result.output)
    }

    @Test
    fun willEmitErrorWhenNoCertificateSelected() = setup(object {
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        initializeProjectFiles()
        writeBuildFile(certificatePath = null)
    } exercise {
        runner.buildAndFail()
    } verify { result ->
        result.output.contains("property 'certificatePath' doesn't have a configured value")
            .assertIsEqualTo(true, result.output)
    }
}
