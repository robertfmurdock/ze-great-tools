package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Tag
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.Thread.sleep
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
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = setOf(settingsFile.name, buildFile.name, ignoreFile.name),
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
