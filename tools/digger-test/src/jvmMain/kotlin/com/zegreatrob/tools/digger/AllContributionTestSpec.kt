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
    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        projectDirectoryPath = projectDir.absolutePath,
        addFileNames = addFileNames,
        commits = commits,
    )

    @Test
    fun `will include all tag segments`() {
        setupWithDefaults()
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
    fun `will consider the path with the most tags, the trunk`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("here's a message"))
        val firstCommit = grgit.head()
        val firstRelease = grgit.addTag("release1")
        delayLongEnoughToAffectGitDate()
        grgit.switchToNewBranch("branch")

        val secondCommit = grgit.addCommitWithMessage("second")
        val midRelease = grgit.addTag("release1-5")
        delayLongEnoughToAffectGitDate()
        grgit.checkout { it.branch = "main" }

        val thirdCommit = grgit.addCommitWithMessage("third")

        val mergeCommit = grgit.mergeInBranch("branch", "merge")
        val release2 = grgit.addTag("release-2")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    firstCommit = thirdCommit,
                    lastCommit = mergeCommit,
                    expectedCommitCount = 2,
                    tag = release2,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    firstCommit = secondCommit,
                    lastCommit = secondCommit,
                    expectedCommitCount = 1,
                    tag = midRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will handle normal merge-into-branch-then-back case well`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("here's a message"))
        val firstCommit = grgit.head()
        val firstRelease = grgit.addTag("release")

        grgit.switchToNewBranch("branch")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch" }
        grgit.addCommitWithMessage("fourth")
        grgit.mergeInBranch("main", "merge-to-branch")

        grgit.checkout { it.branch = "main" }

        val mergeToMainCommit = grgit.mergeInBranch("branch", "merge-to-main")
        val release2 = grgit.addTag("release-2")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    firstCommit = secondCommit,
                    lastCommit = mergeToMainCommit,
                    expectedCommitCount = 5,
                    tag = release2,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will handle normal merge-into-branch-then-ff-back case`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("here's a message"))
        val firstCommit = grgit.head()
        val firstRelease = grgit.addTag("release-1")

        grgit.switchToNewBranch("branch")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")
        val secondRelease = grgit.addTag("release-2")

        grgit.checkout { it.branch = "branch" }
        grgit.addCommitWithMessage("fourth")
        val mergeInBranchCommit = grgit.mergeInBranch("main", "merge-to-branch")

        grgit.checkout { it.branch = "main" }

        grgit.ffOnlyInBranch("branch")
        val thirdRelease = grgit.addTag("release-3")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    firstCommit = secondCommit,
                    lastCommit = mergeInBranchCommit,
                    expectedCommitCount = 3,
                    tag = thirdRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    firstCommit = thirdCommit,
                    lastCommit = thirdCommit,
                    expectedCommitCount = 1,
                    tag = secondRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will handle merge branches`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("first"))
        val firstCommit = grgit.head()

        val firstRelease = grgit.addTag("release")
        grgit.switchToNewBranch("branch1")

        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }

        val thirdCommit = grgit.addCommitWithMessage("third")
        val secondRelease = grgit.addTag("release2")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit = grgit.mergeInBranch("branch1", "merge")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = mergeCommit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 3,
                    tag = thirdRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = thirdCommit,
                    tag = secondRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will handle merge commits on merged branches correctly`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("first"))
        val firstCommit = grgit.head()

        val firstRelease = grgit.addTag("release")
        grgit.switchToNewBranch("branch2")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.switchToNewBranch("branch1")
        grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch2" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fifth")

        grgit.mergeInBranch("branch2", "merge1")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("sixth")

        val merge2Commit = grgit.mergeInBranch("branch1", "merge2")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = merge2Commit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 7,
                    tag = thirdRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `when merging multiple times from same branch, commits are only counted once`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("first"))
        val firstCommit = grgit.head()
        val firstRelease = grgit.addTag("release")

        grgit.switchToNewBranch("branch1")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("third")
        val merge1Commit = grgit.mergeInBranch("branch1", "merge1")
        val secondRelease = grgit.addTag("release2")

        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val merge2Commit = grgit.mergeInBranch("branch1", "merge2")
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = merge2Commit,
                    firstCommit = fourthCommit,
                    expectedCommitCount = 2,
                    tag = thirdRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = merge1Commit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 3,
                    tag = secondRelease,
                    expectedAuthors = defaultAuthors,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = firstRelease,
                    expectedAuthors = defaultAuthors,
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
    fun `will include ease of change`() {
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
                    expectedAuthors = defaultAuthors,
                    expectedEase = 3,
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = tag,
                    expectedAuthors = defaultAuthors,
                    expectedEase = 4,
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will include story ids`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        val tag = grgit.addTag("release")
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    expectedAuthors = defaultAuthors,
                    expectedEase = 3,
                    expectedStoryId = "DOGCOW-18",
                ),
                toContribution(
                    lastCommit = firstCommit,
                    tag = tag,
                    expectedAuthors = defaultAuthors,
                    expectedStoryId = "DOGCOW-17",
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will merge the same story id within a contribution`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-17] -3- here's a message")
        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = defaultAuthors,
                    expectedCommitCount = 2,
                    expectedEase = 3,
                    expectedStoryId = "DOGCOW-17",
                ),
            ),
            parseAll(allOutput),
        )
    }

    @Test
    fun `will merge the different story ids within a contribution`() {
        setupWithOverrides(label = "AwesomeProject")
        val grgit = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("[DOGCOW-18] -3- here's a message")

        val allOutput = runAllContributionData()

        assertEquals(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = defaultAuthors,
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
    fun `will include flatten ease into largest number`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("here's a message -4- more stuff"))
        val firstCommit = grgit.head()
        val secondCommit = grgit.addCommitWithMessage("-3- here's a message")
        val allOutput = runAllContributionData()

        assertEquals(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = defaultAuthors,
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

        initializeGitRepo(commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"))

        val output = runAllContributionData()

        assertEquals(listOf("Major"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceMinorRegex() {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"))

        val output = runAllContributionData()

        assertEquals(listOf("Minor"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplacePatchRegex() {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(commits = listOf("commit 1", "commit (tiny) 2", "commit 3"))
        val output = runAllContributionData()

        assertEquals(listOf("Patch"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceNoneRegex() {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(commits = listOf("commit (no) 1"))
        val output = runAllContributionData()

        assertEquals(listOf("None"), parseAll(output).map { it.semver })
    }

    @Test
    fun canReplaceStoryRegex() {
        setupWithOverrides(storyRegex = ".*-(?<storyId>.*-.*)-.*")

        initializeGitRepo(commits = listOf("commit -CowDog-99- 1"))
        val output = runAllContributionData()

        val contributions = parseAll(output)
        assertEquals(listOf("CowDog-99"), contributions.map { it.storyId })
    }

    @Test
    fun canReplaceEaseRegex() {
        setupWithOverrides(easeRegex = """.*\[(?<ease>[0-5])\].*""")

        initializeGitRepo(commits = listOf("commit [4] 1"))
        val output = runAllContributionData()

        val contributions = parseAll(output)
        assertEquals(listOf(4), contributions.map { it.ease })
    }
}
