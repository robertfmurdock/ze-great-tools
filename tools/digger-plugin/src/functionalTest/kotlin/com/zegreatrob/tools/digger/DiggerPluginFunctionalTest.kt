package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Tag
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DiggerPluginFunctionalTest : CurrentContributionTestSpec {
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
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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

        val firstRelease = grgit.addTag("release")
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
                toContribution(
                    lastCommit = secondCommit,
                    expectedAuthors = listOf(
                        "fourth@gui.io",
                        "funk@test.io",
                        "test@funk.edu",
                        "third@guy.edu",
                    ),
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = listOf(
                        "first@guy.edu",
                        "funk@test.io",
                        "second@gui.io",
                        "test@funk.edu",
                    ),
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }

    private fun toContribution(
        lastCommit: Commit,
        tag: Tag? = null,
        firstCommit: Commit = lastCommit,
        expectedAuthors: List<String>,
        expectedEase: Int? = null,
        expectedStoryId: String? = null,
        expectedCommitCount: Int = 1,
        expectedSemver: String? = null,
        expectedLabel: String = projectDir.name,
    ) = Contribution(
        lastCommit = lastCommit.id,
        dateTime = lastCommit.dateTime?.toInstant()?.toKotlinInstant(),
        firstCommit = firstCommit.id,
        firstCommitDateTime = firstCommit.dateTime?.toInstant()?.toKotlinInstant(),
        authors = expectedAuthors,
        label = expectedLabel,
        ease = expectedEase,
        storyId = expectedStoryId,
        tagName = tag?.name,
        tagDateTime = tag?.dateTime?.toInstant()?.toKotlinInstant(),
        commitCount = expectedCommitCount,
        semver = expectedSemver,
    )

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
                projectDirectoryPath = projectDir.absolutePath,
                addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
                listOf(
                    "here's a message -4- more stuff",
                ),
            )
        val firstCommit = grgit.head()

        val tag = grgit.addTag("release")
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
                toContribution(
                    lastCommit = secondCommit,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedEase = 3,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = tag,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedEase = 4,
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
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
            commits = listOf("[DOGCOW-17] here's a message"),
        )
        val firstCommit = grgit.head()
        val tag = grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("allContributionData", "-q")
            .withProjectDir(projectDir)
            .build()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedEase = 3,
                    expectedStoryId = "DOGCOW-18",
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = tag,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedStoryId = "DOGCOW-17",
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
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
            listOf("[DOGCOW-17] here's a message"),
        )
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
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedCommitCount = 2,
                    expectedEase = 3,
                    expectedStoryId = "DOGCOW-17",
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
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
            commits = listOf("[DOGCOW-17] here's a message"),
        )
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
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedCommitCount = 2,
                    expectedEase = 3,
                    expectedStoryId = "DOGCOW-17, DOGCOW-18",
                    expectedLabel = "AwesomeProject",
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
                projectDirectoryPath = projectDir.absolutePath,
                addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = listOf("funk@test.io", "test@funk.edu"),
                    expectedCommitCount = 2,
                    expectedEase = 4,
                ),
            ),
            parseAll(allOutput.readText()),
        )
    }
}
