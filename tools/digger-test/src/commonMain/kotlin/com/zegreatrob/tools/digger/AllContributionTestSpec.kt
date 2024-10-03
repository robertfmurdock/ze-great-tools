package com.zegreatrob.tools.digger

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.TagRef
import com.zegreatrob.tools.digger.model.Contribution
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.defaultAuthors
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.ffOnlyInBranch
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.removeDirectory
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

interface AllContributionTestSpec : SetupWithOverrides {
    var projectDir: String
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runAllContributionData(): String
    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        directory = projectDir,
        addFileNames = addFileNames,
        commits = commits,
    )

    @BeforeTest
    fun setupProjectDir() {
        projectDir = createTempDirectory()
    }

    fun tearDown() {
        removeDirectory(projectDir)
    }

    @Test
    fun willIncludeAllTagSegments() {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = gitAdapter.addTag("release")
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
    fun willConsiderThePathWithTheMostTagsTheTrunk() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = gitAdapter.addTag("release1")
        delayLongEnoughToAffectGitDate()
        gitAdapter.switchToNewBranch("branch")

        val secondCommit = gitAdapter.addCommitWithMessage("second")
        val midRelease = gitAdapter.addTag("release1-5")
        delayLongEnoughToAffectGitDate()
        gitAdapter.checkout("master")

        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        val mergeCommit = gitAdapter.mergeInBranch("branch", "merge")
        val release2 = gitAdapter.addTag("release-2")

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
    fun willHandleNormalMergeIntoBranchThenBackCaseWell() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = gitAdapter.addTag("release")

        gitAdapter.switchToNewBranch("branch")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.mergeInBranch("master", "merge-to-branch")

        gitAdapter.checkout("master")

        val mergeToMainCommit = gitAdapter.mergeInBranch("branch", "merge-to-main")
        delayLongEnoughToAffectGitDate()
        val release2 = gitAdapter.addTag("release-2")

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
    fun willHandleNormalMergeIntoBranchThenFfBackCase() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = gitAdapter.addTag("release-1")

        gitAdapter.switchToNewBranch("branch")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")
        delayLongEnoughToAffectGitDate()
        val secondRelease = gitAdapter.addTag("release-2")

        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        val mergeInBranchCommit = gitAdapter.mergeInBranch("master", "merge-to-branch")

        gitAdapter.checkout("master")

        gitAdapter.ffOnlyInBranch("branch")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = gitAdapter.addTag("release-3")

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
    fun willHandleMergeBranches() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = gitAdapter.addTag("release")
        gitAdapter.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        delayLongEnoughToAffectGitDate()
        val secondRelease = gitAdapter.addTag("release2")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = gitAdapter.addTag("release3")

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
    fun willIgnoreTagsThatDoNotMatchTagRegex() = runTest {
        setupWithOverrides(
            tagRegex = "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?",
        )
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = gitAdapter.addTag("v1.2.8")
        gitAdapter.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")

        delayLongEnoughToAffectGitDate()
        gitAdapter.addTag("unrelated-tag")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = gitAdapter.addTag("v20.176.37")

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
    fun willHandleMergeCommitsOnMergedBranchesCorrectly() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val firstRelease = gitAdapter.addTag("release")
        gitAdapter.switchToNewBranch("branch2")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.switchToNewBranch("branch1")
        gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch2")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.mergeInBranch("branch2", "merge1")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("sixth")

        val merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = gitAdapter.addTag("release3")

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
    fun whenMergingMultipleTimesFromSameBranchCommitsAreOnlyCountedOnce() = runTest {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val firstRelease = gitAdapter.addTag("release")

        gitAdapter.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")
        val merge1Commit = gitAdapter.mergeInBranch("branch1", "merge1")
        delayLongEnoughToAffectGitDate()
        val secondRelease = gitAdapter.addTag("release2")

        gitAdapter.checkout("branch1")
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = gitAdapter.addTag("release3")

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
        tag: TagRef? = null,
        firstCommit: CommitRef = lastCommit,
        expectedAuthors: List<String>,
        expectedEase: Int? = null,
        expectedStoryId: String? = null,
        expectedCommitCount: Int = 1,
        expectedSemver: String? = null,
        expectedLabel: String = projectDir.split("/").last(),
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
        tagDateTime = tag?.dateTime,
        commitCount = expectedCommitCount,
        semver = expectedSemver,
    )

    @Test
    fun willIncludeEaseOfChange() {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
                "here's a message -4- more stuff",
            ),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!

        val tag = gitAdapter.addTag("release")
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
    fun willIncludeStoryIds() {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        val tag = gitAdapter.addTag("release")
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
    fun willMergeTheSameStoryIdWithinAContribution() {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
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
    fun willMergeTheDifferentStoryIdsWithinAContribution() {
        setupWithOverrides(label = "AwesomeProject")
        val gitAdapter = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
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
    fun willIncludeFlattenEaseIntoLargestNumber() {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message -4- more stuff"))
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
