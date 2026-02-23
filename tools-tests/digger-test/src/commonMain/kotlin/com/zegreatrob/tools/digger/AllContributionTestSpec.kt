package com.zegreatrob.tools.digger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.TagRef
import com.zegreatrob.tools.digger.model.Contribution
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.defaultAuthors
import com.zegreatrob.tools.test.git.ffOnlyInBranch
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.removeDirectory
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlinx.coroutines.delay
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

interface AllContributionTestSpec : SetupWithOverrides {
    var projectDir: String
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runAllContributionData(): String
    private fun <C : Any> longAsyncSetup(
        context: C,
        additionalActions: suspend C.() -> Unit = {},
    ) = asyncSetup(
        context,
        timeoutMs = 180_000,
        additionalActions = additionalActions,
    )
    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        directory = projectDir,
        addFileNames = addFileNames,
        commits = commits,
    )

    @BeforeTest
    fun setupProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun tearDown() {
        removeDirectory(projectDir)
    }

    @Test
    fun willIncludeAllTagSegments() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
    }) {
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

        firstCommit = gitAdapter.show("HEAD")!!

        firstRelease = gitAdapter.addTag("release")
        secondCommit =
            gitAdapter.addCommitWithMessage(
                """here's a message
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
                """.trimMargin(),
            )
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willConsiderThePathWithTheMostTagsTheTrunk() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var midRelease: TagRef
        lateinit var thirdCommit: CommitRef
        lateinit var mergeCommit: CommitRef
        lateinit var release2: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!
        firstRelease = gitAdapter.addTag("release1")
        delay(1100)
        gitAdapter.switchToNewBranch("branch")

        secondCommit = gitAdapter.addCommitWithMessage("second")
        midRelease = gitAdapter.addTag("release1-5")
        delay(1100)
        gitAdapter.checkout("master")

        thirdCommit = gitAdapter.addCommitWithMessage("third")

        mergeCommit = gitAdapter.mergeInBranch("branch", "merge")
        release2 = gitAdapter.addTag("release-2")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willHandleNormalMergeIntoBranchThenBackCaseWell() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var mergeToMainCommit: CommitRef
        lateinit var release2: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!
        firstRelease = gitAdapter.addTag("release")

        gitAdapter.switchToNewBranch("branch")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.mergeInBranch("master", "merge-to-branch")

        gitAdapter.checkout("master")

        mergeToMainCommit = gitAdapter.mergeInBranch("branch", "merge-to-main")
        delay(1100)
        release2 = gitAdapter.addTag("release-2")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willHandleNormalMergeIntoBranchThenFfBackCase() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var thirdCommit: CommitRef
        lateinit var secondRelease: TagRef
        lateinit var mergeInBranchCommit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!
        firstRelease = gitAdapter.addTag("release-1")

        gitAdapter.switchToNewBranch("branch")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        thirdCommit = gitAdapter.addCommitWithMessage("third")
        delay(1100)
        secondRelease = gitAdapter.addTag("release-2")

        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        mergeInBranchCommit = gitAdapter.mergeInBranch("master", "merge-to-branch")

        gitAdapter.checkout("master")

        gitAdapter.ffOnlyInBranch("branch")
        delay(1100)
        thirdRelease = gitAdapter.addTag("release-3")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willHandleMergeBranches() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var thirdCommit: CommitRef
        lateinit var secondRelease: TagRef
        lateinit var mergeCommit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!

        firstRelease = gitAdapter.addTag("release")
        gitAdapter.switchToNewBranch("branch1")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        thirdCommit = gitAdapter.addCommitWithMessage("third")

        delay(1100)
        secondRelease = gitAdapter.addTag("release2")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delay(1100)
        thirdRelease = gitAdapter.addTag("release3")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willIgnoreTagsThatDoNotMatchTagRegex() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var mergeCommit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithOverrides(
            tagRegex = "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?",
        )
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!

        firstRelease = gitAdapter.addTag("v1.2.8")
        gitAdapter.switchToNewBranch("branch1")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")

        delay(1100)
        gitAdapter.addTag("unrelated-tag")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")
        delay(1100)
        thirdRelease = gitAdapter.addTag("v20.176.37")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willHandleMergeCommitsOnMergedBranchesCorrectly() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var merge2Commit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!

        firstRelease = gitAdapter.addTag("release")
        gitAdapter.switchToNewBranch("branch2")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.switchToNewBranch("branch1")
        gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch2")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.mergeInBranch("branch2", "merge1")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("sixth")

        merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delay(1100)
        thirdRelease = gitAdapter.addTag("release3")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun whenMergingMultipleTimesFromSameBranchCommitsAreOnlyCountedOnce() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var firstRelease: TagRef
        lateinit var secondCommit: CommitRef
        lateinit var merge1Commit: CommitRef
        lateinit var secondRelease: TagRef
        lateinit var fourthCommit: CommitRef
        lateinit var merge2Commit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!
        firstRelease = gitAdapter.addTag("release")

        gitAdapter.switchToNewBranch("branch1")
        secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")
        merge1Commit = gitAdapter.mergeInBranch("branch1", "merge1")
        delay(1100)
        secondRelease = gitAdapter.addTag("release2")

        gitAdapter.checkout("branch1")
        fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        merge2Commit = gitAdapter.mergeInBranch("branch1", "merge2")
        delay(1100)
        thirdRelease = gitAdapter.addTag("release3")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willIncludeEaseOfChange() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var tag: TagRef
        lateinit var secondCommit: CommitRef
    }) {
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

        firstCommit = gitAdapter.show("HEAD")!!

        tag = gitAdapter.addTag("release")
        secondCommit =
            gitAdapter.addCommitWithMessage(
                "-3- here's a message",
            )
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willIncludeStoryIds() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var tag: TagRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        firstCommit = gitAdapter.show("HEAD")!!
        tag = gitAdapter.addTag("release")
        secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willMergeTheSameStoryIdWithinAContribution() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("[DOGCOW-17] here's a message"))
        firstCommit = gitAdapter.show("HEAD")!!
        secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-17] -3- here's a message")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willMergeTheDifferentStoryIdsWithinAContribution() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithOverrides(label = "AwesomeProject")
        val gitAdapter = initializeGitRepo(commits = listOf("[DOGCOW-17] here's a message"))
        firstCommit = gitAdapter.show("HEAD")!!
        secondCommit = gitAdapter.addCommitWithMessage("[DOGCOW-18] -3- here's a message")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
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
        )
    }

    @Test
    fun willIncludeFlattenEaseIntoLargestNumber() = longAsyncSetup(object {
        lateinit var firstCommit: CommitRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("here's a message -4- more stuff"))
        firstCommit = gitAdapter.show("HEAD")!!
        secondCommit = gitAdapter.addCommitWithMessage("-3- here's a message")
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).assertIsEqualTo(
            listOf(
                toContribution(
                    lastCommit = secondCommit,
                    firstCommit = firstCommit,
                    expectedAuthors = defaultAuthors,
                    expectedCommitCount = 2,
                    expectedEase = 4,
                ),
            ),
        )
    }

    @Test
    fun canReplaceMajorRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(majorRegex = ".*(big).*")

        initializeGitRepo(commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).map { it.semver }.assertIsEqualTo(listOf("Major"))
    }

    @Test
    fun canReplaceMinorRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).map { it.semver }.assertIsEqualTo(listOf("Minor"))
    }

    @Test
    fun canReplacePatchRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(commits = listOf("commit 1", "commit (tiny) 2", "commit 3"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).map { it.semver }.assertIsEqualTo(listOf("Patch"))
    }

    @Test
    fun canReplaceNoneRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(commits = listOf("commit (no) 1"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        parseAll(allOutput).map { it.semver }.assertIsEqualTo(listOf("None"))
    }

    @Test
    fun canReplaceStoryRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(storyRegex = ".*-(?<storyId>.*-.*)-.*")

        initializeGitRepo(commits = listOf("commit -CowDog-99- 1"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        val contributions = parseAll(allOutput)
        contributions.map { it.storyId }.assertIsEqualTo(listOf("CowDog-99"))
    }

    @Test
    fun canReplaceEaseRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(easeRegex = """.*\[(?<ease>[0-5])\].*""")

        initializeGitRepo(commits = listOf("commit [4] 1"))
    } exercise {
        runAllContributionData()
    } verify { allOutput ->
        val contributions = parseAll(allOutput)
        contributions.map { it.ease }.assertIsEqualTo(listOf(4))
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
}
