package com.zegreatrob.tools.digger

import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Commit
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
    fun `currentContributionData will show authors and co-authors case insensitive`() {
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
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("currentContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()
        assertEquals(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
            parseCurrentAuthors(result.output),
        )
    }

    private fun parseCurrentAuthors(output: String) =
        (JsonSlurper().parse(output.trim().toCharArray()) as Map<*, *>)["authors"]

    private fun parseAll(output: String) =
        (JsonSlurper().parse(output.trim().toCharArray()) as List<*>)

    @Test
    fun `currentContributionData will include authors from multiple commits after last tag`() {
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
                """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                """.trimMargin(),
                """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
                """.trimMargin(),
            ),
        )
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("currentContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                "first@guy.edu",
                "fourth@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(result.output),
        )
    }

    @Test
    fun `currentContributionData will include most recent tag range when head is tagged`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        val grgit = initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        grgit.addTag("earlier")

        grgit.addCommitWithMessage(
            """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
            """.trimMargin(),
        )
        grgit.addCommitWithMessage(
            """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
            """.trimMargin(),
        )
        grgit.addTag("now")

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("currentContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                "fourth@guy.edu",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(result.output),
        )
    }

    @Test
    fun `currentContributionData will not include authors from commits before last tag`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        val grgit = initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )

        grgit.addTag("release")
        grgit.addCommitWithMessage(
            """here's a message
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
            """.trimMargin(),
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("currentContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                "fourth@gui.io",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(result.output),
        )
    }

    @Test
    fun `allContributionData will include all tag segments`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        val grgit = initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val firstCommit = grgit.head()

        grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage(
            """here's a message
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
            """.trimMargin(),
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("allContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                mapOf(
                    "lastCommit" to secondCommit.id,
                    "firstCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toString(),
                    "authors" to listOf(
                        "fourth@gui.io",
                        "funk@test.io",
                        "test@funk.edu",
                        "third@guy.edu",
                    ),
                    "ease" to null,
                ),
                mapOf(
                    "lastCommit" to firstCommit.id,
                    "firstCommit" to firstCommit.id,
                    "dateTime" to firstCommit.dateTime.toString(),
                    "authors" to listOf(
                        "first@guy.edu",
                        "funk@test.io",
                        "second@gui.io",
                        "test@funk.edu",
                    ),
                    "ease" to null,
                ),
            ),
            parseAll(result.output),
        )
    }

    @Test
    fun `allContributionData will include ease of change`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        val grgit = initializeGitRepo(
            listOf(
                "here's a message -4- more stuff",
            ),
        )
        val firstCommit = grgit.head()

        grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage(
            "-3- here's a message",
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("allContributionData", "-q", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toString(),
                    "firstCommit" to secondCommit.id,
                    "ease" to 3,
                ),
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to firstCommit.id,
                    "dateTime" to firstCommit.dateTime.toString(),
                    "firstCommit" to firstCommit.id,
                    "ease" to 4,
                ),
            ),
            parseAll(result.output),
        )
    }

    @Test
    fun `allContributionData will include flatten ease into largest number`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        val grgit = initializeGitRepo(
            listOf(
                "here's a message -4- more stuff",
            ),
        )
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage(
            "-3- here's a message",
        )
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("allContributionData", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toString(),
                    "firstCommit" to firstCommit.id,
                    "ease" to 4,
                ),
            ),
            parseAll(result.output),
        )
    }

    private fun initializeGitRepo(
        commits: List<String> = listOf(),
        initialTag: String? = null,
    ): Grgit {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign()
        grgit.add(fun AddOp.() {
            patterns = setOf(settingsFile.name, buildFile.name, ignoreFile.name)
        })
        if (initialTag != null) {
            grgit.addTag(initialTag)
        }
        commits.forEach { message -> grgit.addCommitWithMessage(message) }

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
        return grgit
    }

    private fun Grgit.addTag(initialTag: String?) {
        tag.add(fun(it: TagAddOp) {
            it.name = initialTag
        })
    }

    private fun Grgit.addCommitWithMessage(message: String): Commit = commit(fun(it: CommitOp) {
        it.author = Person("Funky Testerson", "funk@test.io")
        it.committer = Person("Testy Funkerson", "test@funk.edu")
        it.message = message
    })

    private fun disableGpgSign() {
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
    }
}
