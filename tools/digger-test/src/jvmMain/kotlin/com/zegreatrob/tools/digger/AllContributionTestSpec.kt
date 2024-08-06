package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Tag
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

interface AllContributionTestSpec : SetupWithOverrides {
    var projectDir: File
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runAllContributionData(): String

    @Test
    fun `allContributionData will include all tag segments`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
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
        val allOutput = runAllContributionData()
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
            parseAll(allOutput),
        )
    }

    @Test
    fun `allContributionData will handle merge branches`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("first"),
        )
        val firstCommit = grgit.head()

        val firstRelease = grgit.addTag("release")
        grgit.branch.add { it.name = "branch1" }
        grgit.checkout { it.branch = "branch1" }

        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }

        val thirdCommit = grgit.addCommitWithMessage("third")
        val secondRelease = grgit.addTag("release2")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        grgit.merge {
            it.head = "branch1"
            it.setMode("no-commit")
        }
        val mergeCommit = grgit.addCommitWithMessage("merge")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        val expectedAuthors = listOf(
            "funk@test.io",
            "test@funk.edu",
        )
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = mergeCommit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 3,
                    tag = thirdRelease,
                    expectedAuthors = expectedAuthors,
                ),
                toContribution(
                    lastCommit = thirdCommit,
                    tag = secondRelease,
                    expectedAuthors = expectedAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = expectedAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will handle merge commits on merged branches correctly`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("first"),
        )
        val firstCommit = grgit.head()

        val firstRelease = grgit.addTag("release")
        grgit.branch.add { it.name = "branch2" }
        grgit.checkout { it.branch = "branch2" }
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.branch.add { it.name = "branch1" }
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch2" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fifth")

        grgit.merge {
            it.head = "branch2"
            it.setMode("no-commit")
        }
        grgit.addCommitWithMessage("merge1")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("sixth")
        grgit.merge {
            it.head = "branch1"
            it.setMode("no-commit")
        }
        val merge2Commit = grgit.addCommitWithMessage("merge2")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        val expectedAuthors = listOf(
            "funk@test.io",
            "test@funk.edu",
        )
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = merge2Commit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 7,
                    tag = thirdRelease,
                    expectedAuthors = expectedAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = expectedAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `when merging multiple times from same branch, commits are only counted once`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("first"),
        )
        val firstCommit = grgit.head()

        val firstRelease = grgit.addTag("release")
        grgit.branch.add { it.name = "branch1" }
        grgit.checkout { it.branch = "branch1" }
        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")
        grgit.merge {
            it.head = "branch1"
            it.setMode("no-commit")
        }
        val merge1Commit = grgit.addCommitWithMessage("merge1")
        val secondRelease = grgit.addTag("release2")

        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        grgit.merge {
            it.head = "branch1"
            it.setMode("no-commit")
        }
        val merge2Commit = grgit.addCommitWithMessage("merge2")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        val expectedAuthors = listOf(
            "funk@test.io",
            "test@funk.edu",
        )
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = merge2Commit,
                    firstCommit = fourthCommit,
                    expectedCommitCount = 2,
                    tag = thirdRelease,
                    expectedAuthors = expectedAuthors,
                ),
                toContribution(
                    lastCommit = merge1Commit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 3,
                    tag = secondRelease,
                    expectedAuthors = expectedAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = expectedAuthors,
                ),
            ),
            parseAll(allOutput),
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
        setupWithDefaults()
        val grgit =
            initializeGitRepo(
                projectDirectoryPath = projectDir.absolutePath,
                addFileNames = addFileNames,
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
        val allOutput = runAllContributionData()
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
            parseAll(allOutput),
        )
    }

    @Test
    fun `allContributionData will include story ids`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[DOGCOW-17] here's a message"),
        )
        val firstCommit = grgit.head()
        val tag = grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
        val allOutput = runAllContributionData()
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
            parseAll(allOutput),
        )
    }

    @Test
    fun `allContributionData will merge the same story id within a contribution`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("[DOGCOW-17] here's a message"),
        )
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-17] -3- here's a message")
        val allOutput = runAllContributionData()
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
            parseAll(allOutput),
        )
    }

    @Test
    fun `allContributionData will merge the different story ids within a contribution`() {
        setupWithOverrides(label = "AwesomeProject")
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[DOGCOW-17] here's a message"),
        )
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")

        val allOutput = runAllContributionData()

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
            parseAll(allOutput),
        )
    }

    @Test
    fun `allContributionData will include flatten ease into largest number`() {
        setupWithDefaults()
        val grgit =
            initializeGitRepo(
                projectDirectoryPath = projectDir.absolutePath,
                addFileNames = addFileNames,
                listOf(
                    "here's a message -4- more stuff",
                ),
            )
        val firstCommit = grgit.head()
        val secondCommit =
            grgit.addCommitWithMessage(
                "-3- here's a message",
            )
        val allOutput = runAllContributionData()

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
            parseAll(allOutput),
        )
    }

    @Test
    fun canReplaceMajorRegex() {
        setupWithOverrides(majorRegex = ".*(big).*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
        )

        val output = runAllContributionData()

        assertEquals(listOf("Major"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceMinorRegex() {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
        )

        val output = runAllContributionData()

        assertEquals(listOf("Minor"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplacePatchRegex() {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit 1", "commit (tiny) 2", "commit 3"),
        )
        val output = runAllContributionData()

        assertEquals(listOf("Patch"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceNoneRegex() {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit (no) 1"),
        )
        val output = runAllContributionData()

        assertEquals(listOf("None"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceStoryRegex() {
        setupWithOverrides(storyRegex = ".*-(?<storyId>.*-.*)-.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit -CowDog-99- 1"),
        )
        val output = runAllContributionData()

        val contributions = parseAll(output)
        assertEquals(listOf("CowDog-99"), contributions.map { it.storyId })
    }

    @Test
    fun canReplaceEaseRegex() {
        setupWithOverrides(easeRegex = """.*\[(?<ease>[0-5])\].*""")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit [4] 1"),
        )
        val output = runAllContributionData()

        val contributions = parseAll(output)
        assertEquals(listOf(4), contributions.map { it.ease })
    }
}
