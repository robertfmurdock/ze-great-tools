package com.zegreatrob.certifier.functional

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains

class CertifierPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }

    @BeforeTest
    fun setup() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
    }

    @Test
    fun `can run install cert task`() {
        val certificatePath = this.javaClass.getResource("/localhost.crt")?.toURI()?.path

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

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("installCert")
        runner.withProjectDir(projectDir)
        runner.build()
    }

    @Test
    fun `will emit error when no jdk selected`() {
        val certificatePath = this.javaClass.getResource("/localhost.crt")?.toURI()?.path

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

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("installCert")
        runner.withProjectDir(projectDir)
        val result = runner.buildAndFail()
        assertContains(result.output, "property 'jdkSelector' doesn't have a configured value")
    }

    @Test
    fun `will emit error when no certificate selected`() {
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

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("installCert")
        runner.withProjectDir(projectDir)
        val result = runner.buildAndFail()
        assertContains(result.output, "property 'certificatePath' doesn't have a configured value")
    }
}
