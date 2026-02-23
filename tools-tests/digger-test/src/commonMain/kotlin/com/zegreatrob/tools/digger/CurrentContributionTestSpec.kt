package com.zegreatrob.tools.digger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.TagRef
import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import com.zegreatrob.tools.digger.model.Contribution
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.defaultAuthors
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.removeDirectory
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlinx.coroutines.delay
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

interface CurrentContributionTestSpec : SetupWithOverrides {
    var projectDir: String
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runCurrentContributionData(): String
    private fun <C : Any> longAsyncSetup(
        context: C,
        additionalActions: suspend C.() -> Unit = {},
    ) = asyncSetup(
        context,
        timeoutMs = 180_000,
        additionalActions = additionalActions,
    )

    @BeforeTest
    fun setupProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun tearDown() {
        removeDirectory(projectDir)
    }

    @BeforeTest
    fun checkPrerequisites() {
        getEnvironmentVariable("GIT_CONFIG_GLOBAL").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
        getEnvironmentVariable("GIT_CONFIG_SYSTEM").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
    }

    @Test
    fun currentContributionDataWillShowAuthorsAndCoAuthorsCaseInsensitive() = longAsyncSetup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
                """here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseCurrentAuthors(output).assertIsEqualTo(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
        )
    }

    @Test
    fun whenLabelIsSetWillApplyItToContribution() = longAsyncSetup(object {
        val label = "extraSpecialLabel"
    }) {
        setupWithOverrides(label = label)

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("here's a message"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.label.assertIsEqualTo(label)
    }

    @Test
    fun whenLabelIsNotSetWillUseDirectoryName() = longAsyncSetup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("here's a message"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.label.assertIsEqualTo(projectDir.split("/").last())
    }

    @Test
    fun whenIncludedCurrentContributionDataWillShowSemverLevel() = longAsyncSetup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
                """[patch] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseSemver(output).assertIsEqualTo("Patch")
        parseStoryId(output).assertIsEqualTo(null)
    }

    @Test
    fun whenCurrentContributionDataIncludesMultipleSemversUsesLargest() = longAsyncSetup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
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
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseSemver(output).assertIsEqualTo("Major")
        parseStoryId(output).assertIsEqualTo(null)
    }

    @Test
    fun currentContributionDataWillIncludeAuthorsFromMultipleCommitsAfterLastTag() = longAsyncSetup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
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
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseCurrentAuthors(output).assertIsEqualTo(
            listOf(
                "first@guy.edu",
                "fourth@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
        )
    }

    @Test
    fun currentContributionDataWillIncludeMostRecentTagRangeWhenHeadIsTagged() = longAsyncSetup(object {}) {
        setupWithDefaults()

        val gitAdapter = initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
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

        gitAdapter.addTag("earlier")

        gitAdapter.addCommitWithMessage(
            """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
            """.trimMargin(),
        )
        gitAdapter.addCommitWithMessage(
            """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
            """.trimMargin(),
        )
        gitAdapter.addTag("now")
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseCurrentAuthors(output).assertIsEqualTo(
            listOf(
                "fourth@guy.edu",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
        )
    }

    @Test
    fun whenHeadIsTaggedCurrentContributionDataWillUseIncludeTagInfo() = longAsyncSetup(object {
        lateinit var nowTag: TagRef
    }) {
        setupWithDefaults()

        val gitAdapter = initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
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

        gitAdapter.addTag("earlier")

        gitAdapter.addCommitWithMessage(
            """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
            """.trimMargin(),
        )
        gitAdapter.addCommitWithMessage(
            """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
            """.trimMargin(),
        )
        delay(1000)
        nowTag = gitAdapter.addTag("now")
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        val contribution = parseContribution(output)
        contribution?.tagName.assertIsEqualTo(nowTag.name)
        contribution?.tagDateTime.assertIsEqualTo(nowTag.dateTime)
    }

    @Test
    fun currentContributionDataWillNotIncludeAuthorsFromCommitsBeforeLastTag() = longAsyncSetup(object {}) {
        setupWithDefaults()

        val gitAdapter = initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf(
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

        gitAdapter.addTag("release")
        gitAdapter.addCommitWithMessage(
            """here's a message
                |
                |
                |Co-authored-by: Third Guy <Third@Guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
            """.trimMargin(),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseCurrentAuthors(output).assertIsEqualTo(
            listOf(
                "fourth@gui.io",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
        )
    }

    @Test
    fun canReplaceMajorRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(majorRegex = ".*(big).*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.semver.assertIsEqualTo("Major")
    }

    @Test
    fun canReplaceMinorRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.semver.assertIsEqualTo("Minor")
    }

    @Test
    fun canReplacePatchRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit 1", "commit (tiny) 2", "commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.semver.assertIsEqualTo("Patch")
    }

    @Test
    fun canReplaceNoneRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit (no) 1"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        parseContribution(output)?.semver.assertIsEqualTo("None")
    }

    @Test
    fun canReplaceStoryRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(storyRegex = ".*-(?<storyId>.*-.*)-.*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit -CowDog-99- 1"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        val contribution = parseContribution(output)
        contribution?.storyId.assertIsEqualTo("CowDog-99")
    }

    @Test
    fun canReplaceEaseRegex() = longAsyncSetup(object {}) {
        setupWithOverrides(easeRegex = """.*\[(?<ease>[0-5])\].*""")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit [4] 1"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        val contribution = parseContribution(output)
        contribution?.ease.assertIsEqualTo(4)
    }

    @Test
    fun willHandleMergeCommitsOnMergedBranchesCorrectly() = longAsyncSetup(object {
        lateinit var merge2Commit: CommitRef
        lateinit var secondRelease: TagRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        gitAdapter.addTag("release")
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
        secondRelease = gitAdapter.addTag("release2")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        parseContribution(allOutput).assertIsEqualTo(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                tag = secondRelease,
                expectedAuthors = defaultAuthors,
            ),
        )
    }

    @Test
    fun willIgnoreTagsThatDoNotMatchRegex() = longAsyncSetup(object {
        lateinit var merge2Commit: CommitRef
        lateinit var secondCommit: CommitRef
    }) {
        setupWithOverrides(
            tagRegex = "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?",
        )
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        gitAdapter.addTag("v1.0.0")
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
        gitAdapter.addTag("ignore-me")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        parseContribution(allOutput).assertIsEqualTo(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                expectedAuthors = defaultAuthors,
            ),
        )
    }

    @Test
    fun willCorrectlyUnderstandLongerRunningBranch() = longAsyncSetup(object {
        lateinit var secondCommit: CommitRef
        lateinit var lastCommit: CommitRef
        lateinit var thirdRelease: TagRef
    }) {
        setupWithDefaults()
        val gitAdapter = initializeGitRepo(listOf("first"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        gitAdapter.addTag("release")
        gitAdapter.switchToNewBranch("branch")
        secondCommit = gitAdapter.addCommitWithMessage("second")
        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("third")
        delay(1100)
        gitAdapter.addTag("release2")
        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.addCommitWithMessage("fifth")
        gitAdapter.checkout("master")
        gitAdapter.mergeInBranch("branch", "merge")
        lastCommit = gitAdapter.addCommitWithMessage("sixth")
        delay(1100)
        thirdRelease = gitAdapter.addTag("release3")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        parseContribution(allOutput).assertIsEqualTo(
            toContribution(
                lastCommit = lastCommit,
                firstCommit = secondCommit,
                expectedCommitCount = 5,
                tag = thirdRelease,
                expectedAuthors = defaultAuthors,
            ),
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
    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        directory = projectDir,
        addFileNames = addFileNames,
        commits = commits,
    )
}
