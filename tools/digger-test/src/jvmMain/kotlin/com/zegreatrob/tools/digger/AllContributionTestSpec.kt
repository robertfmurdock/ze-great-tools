package com.zegreatrob.tools.digger

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.digger.model.Contribution
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
import com.zegreatrob.tools.test.git.defaultAuthors
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.ffOnlyInBranch
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlinx.datetime.toKotlinInstant
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
        directory = projectDir.absolutePath,
        addFileNames = addFileNames,
        commits = commits,
    )

    @Test
    fun `will include all tag segments`() {
        setupWithDefaults()
        val (grgit, gitAdapter) = initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = grgit.addTag("release")
        val secondCommit =
            gitAdapter.addCommitWithMessage(
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
        val (grgit, gitAdapter) = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = grgit.addTag("release1")
        delayLongEnoughToAffectGitDate()
        grgit.switchToNewBranch("branch")

        val secondCommit = gitAdapter.addCommitWithMessage("second")
        val midRelease = grgit.addTag("release1-5")
        delayLongEnoughToAffectGitDate()
        grgit.checkout { it.branch = "master" }

        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        val mergeCommit = gitAdapter.mergeInBranch("branch", "merge")
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
        val (grgit, gitAdapter) = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = grgit.addTag("release")

        grgit.switchToNewBranch("branch")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.checkout { it.branch = "master" }
        gitAdapter.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch" }
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.mergeInBranch("master", "merge-to-branch")

        grgit.checkout { it.branch = "master" }

        val mergeToMainCommit = gitAdapter.mergeInBranch("branch", "merge-to-main")
        delayLongEnoughToAffectGitDate()
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
        val (grgit, gitAdapter) = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = grgit.addTag("release-1")

        grgit.switchToNewBranch("branch")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.checkout { it.branch = "master" }
        val thirdCommit = gitAdapter.addCommitWithMessage("third")
        delayLongEnoughToAffectGitDate()
        val secondRelease = grgit.addTag("release-2")

        grgit.checkout { it.branch = "branch" }
        gitAdapter.addCommitWithMessage("fourth")
        val mergeInBranchCommit = gitAdapter.mergeInBranch("master", "merge-to-branch")

        grgit.checkout { it.branch = "master" }

        grgit.ffOnlyInBranch("branch")
        delayLongEnoughToAffectGitDate()
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
    fun willHandleMergeBranches() {
        setupWithDefaults()
        val (grgit, gitAdapter) = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = grgit.addTag("release")
        grgit.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.checkout { it.branch = "master" }
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        delayLongEnoughToAffectGitDate()
        val secondRelease = grgit.addTag("release2")
        grgit.checkout { it.branch = "branch1" }
        gitAdapter.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "master" }
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delayLongEnoughToAffectGitDate()
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
    fun willIgnoreTagsThatDoNotMatchTagRegex() {
        setupWithOverrides(
            tagRegex = "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?",
        )
        val (grgit, gitAdapter) = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = grgit.addTag("v1.2.8")
        grgit.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.checkout { it.branch = "master" }
        gitAdapter.addCommitWithMessage("third")

        delayLongEnoughToAffectGitDate()
        grgit.addTag("unrelated-tag")
        grgit.checkout { it.branch = "branch1" }
        gitAdapter.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "master" }
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = grgit.addTag("v20.176.37")

        val allOutput = runAllContributionData()
        assertEquals(
            listOf(
                toContribution(
                    lastCommit = mergeCommit,
                    firstCommit = secondCommit,
                    expectedCommitCount = 4,
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
    fun `will handle merge commits on merged branches correctly`() {
        setupWithDefaults()
        val (grgit, gitAdapter) = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = grgit.addTag("release")
        grgit.switchToNewBranch("branch2")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.switchToNewBranch("branch1")
        gitAdapter.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch2" }
        gitAdapter.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "branch1" }
        gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.mergeInBranch("branch2", "merge1")

        grgit.checkout { it.branch = "master" }
        gitAdapter.addCommitWithMessage("sixth")

        val merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
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
        val (grgit, gitAdapter) = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = grgit.addTag("release")

        grgit.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        grgit.checkout { it.branch = "master" }
        gitAdapter.addCommitWithMessage("third")
        val merge1Commit = gitAdapter.mergeInBranch("branch1", "merge1")
        delayLongEnoughToAffectGitDate()
        val secondRelease = grgit.addTag("release2")

        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "master" }
        val merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
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
        lastCommit: CommitRef,
        tag: Tag? = null,
        firstCommit: CommitRef = lastCommit,
        expectedAuthors: List<String>,
        expectedEase: Int? = null,
        expectedStoryId: String? = null,
        expectedCommitCount: Int = 1,
        expectedSemver: String? = null,
        expectedLabel: String = projectDir.name,
    ) = Contribution(
        lastCommit = lastCommit.id,
        dateTime = lastCommit.dateTime,
        firstCommit = firstCommit.id,
        firstCommitDateTime = firstCommit.dateTime,
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
        val (grgit, gitAdapter) = initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf(
                "here's a message -4- more stuff",
            ),
        )
        val firstCommit = gitAdapter.show("HEAD")!!

        val tag = grgit.addTag("release")
        val secondCommit =
            gitAdapter.addCommitWithMessage(
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
        val (grgit, gitAdapter) = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        val firstCommit = gitAdapter.show("HEAD")!!
        val tag = grgit.addTag("release")
        val secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
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
        val (_, gitAdapter) = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        val firstCommit = gitAdapter.show("HEAD")!!
        val secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-17] -3- here's a message")
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
        val (_, gitAdapter) = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        val firstCommit = gitAdapter.show("HEAD")!!
        val secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-18] -3- here's a message")

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
        val (_, gitAdapter) = initializeGitRepo(listOf("here's a message -4- more stuff"))
        val firstCommit = gitAdapter.show("HEAD")!!
        val secondCommit = gitAdapter.addCommitWithMessage("-3- here's a message")
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
