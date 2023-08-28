package digger

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.CheckoutOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.RemoteAddOp
import org.ajoberstar.grgit.operation.TagAddOp
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DiggerPluginFunctionalTest {

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
    fun `will show authors and co-authors`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("diggerGitDescription", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            """test@funk.edu
                |funk@test.io
                |first@guy.edu
                |second@gui.io
            """.trimMargin(),
            result.output.trim(),
        )
    }

    private fun initializeGitRepo(
        commits: List<String> = listOf(),
        initialTag: String? = null,
    ) {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign()
        grgit.add(fun AddOp.() {
            patterns = setOf(settingsFile.name, buildFile.name, ignoreFile.name)
        })
        if (initialTag != null) {
            grgit.tag.add(fun(it: TagAddOp) {
                it.name = initialTag
            })
        }
        commits.forEach { message ->
            grgit.commit(fun(it: CommitOp) {
                it.author = Person("Funky Testerson", "funk@test.io")
                it.committer = Person("Testy Funkerson", "test@funk.edu")
                it.message = message
            })
        }

        grgit.remote.add(fun RemoteAddOp.() {
            this.name = "origin"
            this.url = projectDir.absolutePath
        })
        grgit.checkout(fun CheckoutOp.() {
            branch = "main"
            createBranch = true
        })
        grgit.pull()
        grgit.branch.change(fun BranchChangeOp.() {
            this.name = "main"
            this.startPoint = "origin/main"
            this.mode = BranchChangeOp.Mode.TRACK
        })
    }

    private fun disableGpgSign() {
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
    }
}
