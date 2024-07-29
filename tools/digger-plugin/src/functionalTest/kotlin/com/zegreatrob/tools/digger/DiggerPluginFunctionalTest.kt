package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import groovy.json.JsonSlurper
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.Tag
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
import java.lang.Thread.sleep
import java.time.ZonedDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DiggerPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }
    private val currentOutput by lazy { projectDir.resolve("build/digger/current.json") }
    private val allOutput by lazy { projectDir.resolve("build/digger/all.json") }

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
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
            parseCurrentAuthors(currentOutput.readText()),
        )
    }

    @Test
    fun `when included currentContributionData will show semver level`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        initializeGitRepo(
            listOf(
                """[patch] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals("Patch", parseSemver(currentOutput.readText()))
        assertEquals(null, parseStoryId(currentOutput.readText()))
    }

    @Test
    fun `when currentContributionData includes multiple semvers uses largest`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )

        initializeGitRepo(
            listOf(
                """[major] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
                """[minor] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals("Major", parseSemver(currentOutput.readText()))
        assertEquals(null, parseStoryId(currentOutput.readText()))
    }

    private fun parseCurrentAuthors(output: String) = parseContribution(output)?.authors

    private fun parseSemver(output: String) = parseContribution(output)?.semver

    private fun parseStoryId(output: String) = parseContribution(output)?.storyId

    private fun parseAll(output: String) = (JsonSlurper().parse(output.trim().toCharArray()) as List<*>)

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
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        assertEquals(
            listOf(
                "first@guy.edu",
                "fourth@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(currentOutput.readText()),
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

        val grgit =
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

        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        assertEquals(
            listOf(
                "fourth@guy.edu",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(currentOutput.readText()),
        )
    }

    @Test
    fun `when head is tagged currentContributionData will use include tag info`() {
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
        sleep(1000)
        val nowTag = grgit.addTag("now")

        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        val contribution = parseContribution(currentOutput.readText())
        assertEquals(nowTag?.name, contribution?.tagName)
        assertEquals(nowTag?.dateTime?.toInstant()?.toKotlinInstant(), contribution?.tagDateTime)
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

        val grgit =
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

        grgit.addTag("release")
        grgit.addCommitWithMessage(
            """here's a message
                |
                |
                |Co-authored-by: Third Guy <Third@Guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
            """.trimMargin(),
        )

        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("currentContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        assertEquals(
            listOf(
                "fourth@gui.io",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(currentOutput.readText()),
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

        val grgit =
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
        val firstCommit = grgit.head()

        grgit.addTag("release")
        val secondCommit =
            grgit.addCommitWithMessage(
                """here's a message
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
                """.trimMargin(),
            )
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                mapOf(
                    "lastCommit" to secondCommit.id,
                    "firstCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommitDateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "authors" to
                        listOf(
                            "fourth@gui.io",
                            "funk@test.io",
                            "test@funk.edu",
                            "third@guy.edu",
                        ),
                    "label" to projectDir.name,
                ),
                mapOf(
                    "lastCommit" to firstCommit.id,
                    "firstCommit" to firstCommit.id,
                    "dateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "authors" to
                        listOf(
                            "first@guy.edu",
                            "funk@test.io",
                            "second@gui.io",
                            "test@funk.edu",
                        ),
                    "label" to projectDir.name,
                ),
            ),
            parseAll(allOutput.readText()),
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

        val grgit =
            initializeGitRepo(
                listOf(
                    "here's a message -4- more stuff",
                ),
            )
        val firstCommit = grgit.head()

        grgit.addTag("release")
        val secondCommit =
            grgit.addCommitWithMessage(
                "-3- here's a message",
            )
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to secondCommit.id,
                    "firstCommitDateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "ease" to 3,
                    "label" to projectDir.name,
                ),
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to firstCommit.id,
                    "dateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to firstCommit.id,
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "ease" to 4,
                    "label" to projectDir.name,
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }

    @Test
    fun `allContributionData will include story ids`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )
        val grgit = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to secondCommit.id,
                    "firstCommitDateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "ease" to 3,
                    "storyId" to "DOGCOW-18",
                    "label" to projectDir.name,
                ),
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to firstCommit.id,
                    "dateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to firstCommit.id,
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "storyId" to "DOGCOW-17",
                    "label" to projectDir.name,
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }

    @Test
    fun `allContributionData will merge the same story id within a contribution`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            """.trimIndent(),
        )
        val grgit = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-17] -3- here's a message")
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to firstCommit.id,
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "ease" to 3,
                    "storyId" to "DOGCOW-17",
                    "label" to projectDir.name,
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }

    @Test
    fun `allContributionData will merge the different story ids within a contribution`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.digger")
            }
            digger {
                label.set("AwesomeProject")
            }
            """.trimIndent(),
        )
        val grgit = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")

        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to firstCommit.id,
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "ease" to 3,
                    "storyId" to "DOGCOW-17, DOGCOW-18",
                    "label" to "AwesomeProject",
                ),
            ),
            parseAll(allOutput.readText()),
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

        val grgit =
            initializeGitRepo(
                listOf(
                    "here's a message -4- more stuff",
                ),
            )
        val firstCommit = grgit.head()
        val secondCommit =
            grgit.addCommitWithMessage(
                "-3- here's a message",
            )
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()

        assertEquals(
            listOf(
                mapOf(
                    "authors" to listOf("funk@test.io", "test@funk.edu"),
                    "lastCommit" to secondCommit.id,
                    "dateTime" to secondCommit.dateTime.toKotlinInstantString(),
                    "firstCommit" to firstCommit.id,
                    "firstCommitDateTime" to firstCommit.dateTime.toKotlinInstantString(),
                    "ease" to 4,
                    "label" to projectDir.name,
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }

    private fun ZonedDateTime.toKotlinInstantString() = toInstant()?.toKotlinInstant()?.toString()

    private fun initializeGitRepo(
        commits: List<String> = listOf(),
        initialTag: String? = null,
    ): Grgit {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign()
        grgit.add(
            fun AddOp.() {
                patterns = setOf(settingsFile.name, buildFile.name, ignoreFile.name)
            },
        )
        if (initialTag != null) {
            grgit.addTag(initialTag)
        }
        commits.forEach { message -> grgit.addCommitWithMessage(message) }

        grgit.remote.add(
            fun RemoteAddOp.() {
                this.name = "origin"
                this.url = projectDir.absolutePath
            },
        )
        grgit.checkout(
            fun CheckoutOp.() {
                branch = "main"
                createBranch = true
            },
        )
        grgit.pull()
        grgit.branch.change(
            fun BranchChangeOp.() {
                this.name = "main"
                this.startPoint = "origin/main"
                this.mode = BranchChangeOp.Mode.TRACK
            },
        )
        return grgit
    }

    private fun Grgit.addTag(initialTag: String?): Tag? = tag.add(
        fun(it: TagAddOp) {
            it.name = initialTag
        },
    )

    private fun Grgit.addCommitWithMessage(message: String): Commit =
        commit(
            fun(it: CommitOp) {
                it.author = Person("Funky Testerson", "funk@test.io")
                it.committer = Person("Testy Funkerson", "test@funk.edu")
                it.message = message
            },
        )

    private fun disableGpgSign() {
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
    }
}
