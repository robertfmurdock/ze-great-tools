package tagger

import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Grgit
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import kotlin.test.assertEquals

class TaggerPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test
    fun `can run task`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('com.zegreatrob.tools.tagger')
            }
        """.trimIndent()
        )

        initializeGitRepo()
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(result.output.trim(), "v0.0.0-SNAPSHOT")
    }

    private fun initializeGitRepo() {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
        grgit.add(mapOf("patterns" to listOf(settingsFile.absolutePath, buildFile.absolutePath)))
        grgit.commit(mapOf("message" to "test commit"))

        grgit.checkout(mapOf("branch" to "main", "createBranch" to true))
    }
}
