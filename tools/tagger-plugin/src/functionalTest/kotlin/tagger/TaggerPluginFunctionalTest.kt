package tagger

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.TagAddOp
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class TaggerPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test
    fun `calculating version with no tags produces patch snapshot`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('com.zegreatrob.tools.tagger')
            }
            tagger {
                releaseBranch = "master"
            }

            """.trimIndent()
        )

        initializeGitRepo(listOf("[patch] commit 1", "[patch] commit 2"))
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("0.0.0-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `calculating version with all patch commits only increments patch`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('com.zegreatrob.tools.tagger')
            }
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent()
        )

        initializeGitRepo(listOf("[patch] commit 1", "[patch] commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `calculating version with one minor commits only increments minor`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('com.zegreatrob.tools.tagger')
            }
            
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent()
        )

        initializeGitRepo(listOf("[patch] commit 1", "[minor] commit 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.3.0-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `calculating version with one major commits only increments major`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('com.zegreatrob.tools.tagger')
            }
            
            tagger {
                releaseBranch = "master"
            }

            """.trimIndent()
        )

        initializeGitRepo(listOf("[major] commit 1", "[minor] commit 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()
        assertEquals("2.0.0-SNAPSHOT", result.output.trim())
    }

    private fun initializeGitRepo(additionalCommits: List<String> = listOf(), initialTag: String? = null) {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
        grgit.add(fun(it: AddOp) {
            it.patterns = setOf(settingsFile.absolutePath, buildFile.absolutePath)
        })

        grgit.commit(fun(it: CommitOp) { it.message = "test commit" })
        if (initialTag != null) {
            grgit.tag.add(fun(it: TagAddOp) {
                it.name = initialTag
            })
        }
        additionalCommits.forEach { message ->
            grgit.commit(fun(it: CommitOp) {
                it.message = message
            })
        }
        grgit.checkout(mapOf("branch" to "main", "createBranch" to true))
    }
}
