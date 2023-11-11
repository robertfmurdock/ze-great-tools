package com.zegreatrob.certifier.functional

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

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
    fun `calculating version with no tags produces zero version`() {
        val certificatePath = this.javaClass.getResource("/localhost.crt")?.toURI()?.path

        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.certifier")
            }

            tasks {
                register("installCert", com.zegreatrob.tools.certifier.InstallCertificate::class) {
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
}
