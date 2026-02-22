package com.zegreatrob.tools.digger

import com.zegreatrob.testmints.setup
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
import com.zegreatrob.tools.test.git.sleep
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

interface CurrentContributionTestSpec : SetupWithOverrides {
    var projectDir: String
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runCurrentContributionData(): String

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
        assertEquals(
            "/dev/null",
            getEnvironmentVariable("GIT_CONFIG_GLOBAL"),
            "Ensure this is set for the test to work as intended",
        )
        assertEquals(
            "/dev/null",
            getEnvironmentVariable("GIT_CONFIG_SYSTEM"),
            "Ensure this is set for the test to work as intended",
        )
    }

    @Test
    fun currentContributionDataWillShowAuthorsAndCoAuthorsCaseInsensitive() = setup(object {}) {
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
        assertEquals(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun whenLabelIsSetWillApplyItToContribution() = setup(object {
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
        assertEquals(
            label,
            parseContribution(output)?.label,
        )
    }

    @Test
    fun whenLabelIsNotSetWillUseDirectoryName() = setup(object {}) {
        setupWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("here's a message"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        assertEquals(
            projectDir.split("/").last(),
            parseContribution(output)?.label,
        )
    }

    @Test
    fun whenIncludedCurrentContributionDataWillShowSemverLevel() = setup(object {}) {
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
        assertEquals("Patch", parseSemver(output))
        assertEquals(null, parseStoryId(output))
    }

    @Test
    fun whenCurrentContributionDataIncludesMultipleSemversUsesLargest() = setup(object {}) {
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
        assertEquals("Major", parseSemver(output))
        assertEquals(null, parseStoryId(output))
    }

    @Test
    fun currentContributionDataWillIncludeAuthorsFromMultipleCommitsAfterLastTag() = setup(object {}) {
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
        assertEquals(
            listOf(
                "first@guy.edu",
                "fourth@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun currentContributionDataWillIncludeMostRecentTagRangeWhenHeadIsTagged() = setup(object {}) {
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
        assertEquals(
            listOf(
                "fourth@guy.edu",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun whenHeadIsTaggedCurrentContributionDataWillUseIncludeTagInfo() = setup(object {
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
        sleep(1000)
        nowTag = gitAdapter.addTag("now")
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        val contribution = parseContribution(output)
        assertEquals(nowTag.name, contribution?.tagName)
        assertEquals(nowTag.dateTime, contribution?.tagDateTime)
    }

    @Test
    fun currentContributionDataWillNotIncludeAuthorsFromCommitsBeforeLastTag() = setup(object {}) {
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
        assertEquals(
            listOf(
                "fourth@gui.io",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun canReplaceMajorRegex() = setup(object {}) {
        setupWithOverrides(majorRegex = ".*(big).*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        assertEquals("Major", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceMinorRegex() = setup(object {}) {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        assertEquals("Minor", parseContribution(output)?.semver)
    }

    @Test
    fun canReplacePatchRegex() = setup(object {}) {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit 1", "commit (tiny) 2", "commit 3"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        assertEquals("Patch", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceNoneRegex() = setup(object {}) {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(
            directory = projectDir,
            addFileNames = addFileNames,
            commits = listOf("commit (no) 1"),
        )
    } exercise {
        runCurrentContributionData()
    } verify { output ->
        assertEquals("None", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceStoryRegex() = setup(object {}) {
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
        assertEquals("CowDog-99", contribution?.storyId)
    }

    @Test
    fun canReplaceEaseRegex() = setup(object {}) {
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
        assertEquals(4, contribution?.ease)
    }

    @Test
    fun willHandleMergeCommitsOnMergedBranchesCorrectly() = setup(object {
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
        sleep(1100)
        secondRelease = gitAdapter.addTag("release2")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        assertEquals(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                tag = secondRelease,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
        )
    }

    @Test
    fun willIgnoreTagsThatDoNotMatchRegex() = setup(object {
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
        sleep(1100)
        gitAdapter.addTag("ignore-me")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        assertEquals(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
        )
    }

    @Test
    fun willCorrectlyUnderstandLongerRunningBranch() = setup(object {
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
        sleep(1100)
        gitAdapter.addTag("release2")
        gitAdapter.checkout("branch")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.addCommitWithMessage("fifth")
        gitAdapter.checkout("master")
        gitAdapter.mergeInBranch("branch", "merge")
        lastCommit = gitAdapter.addCommitWithMessage("sixth")
        sleep(1100)
        thirdRelease = gitAdapter.addTag("release3")
    } exercise {
        runCurrentContributionData()
    } verify { allOutput ->
        assertEquals(
            toContribution(
                lastCommit = lastCommit,
                firstCommit = secondCommit,
                expectedCommitCount = 5,
                tag = thirdRelease,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
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
