package com.zegreatrob.certifier.functional

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class CertifierPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }

    @Test
    fun canRunInstallCertTask() = setup(object {
        val certificatePath = this@CertifierPluginFunctionalTest.javaClass.getResource("/localhost.crt")?.toURI()?.path
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.certifier")
            }

            tasks {
                installCert {
                    jdkSelector = "20"
                    certificatePath = "$certificatePath"
                }
            }

            """.trimIndent(),
        )
    } exercise {
        runner.build()
    } verify {
    }

    @Test
    fun willEmitErrorWhenNoJdkSelected() = setup(object {
        val certificatePath = this@CertifierPluginFunctionalTest.javaClass.getResource("/localhost.crt")?.toURI()?.path
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.certifier")
            }

            tasks {
                installCert {
                    certificatePath = "$certificatePath"
                }
            }

            """.trimIndent(),
        )
    } exercise {
        runner.buildAndFail()
    } verify { result ->
        assertContains(result.output, "property 'jdkSelector' doesn't have a configured value")
    }

    @Test
    fun willEmitErrorWhenNoCertificateSelected() = setup(object {
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("installCert")
            .withProjectDir(projectDir)
    }) {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.certifier")
            }

            tasks {
                installCert {
                    jdkSelector = "20"
                }
            }
            """.trimIndent(),
        )
    } exercise {
        runner.buildAndFail()
    } verify { result ->
        assertContains(result.output, "property 'certificatePath' doesn't have a configured value")
    }
}
