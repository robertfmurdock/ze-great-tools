package com.zegreatrob.tools.tagger

import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

interface CalculateVersionTestSpec {
    var projectDir: String
    val addFileNames: Set<String>

    @BeforeTest
    fun setUpProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun deleteProjectDir() {
        removeDirectory(projectDir)
    }

    fun configureWithDefaults()

    fun configureWithOverrides(
        implicitPatch: Boolean? = null,
        disableDetached: Boolean? = null,
        majorRegex: String? = null,
        minorRegex: String? = null,
        patchRegex: String? = null,
        versionRegex: String? = null,
        noneRegex: String? = null,
        forceSnapshot: Boolean? = null,
    )

    fun initializeGitRepo(
        commits: List<String>,
        initialTag: String? = null,
        remoteUrl: String = projectDir,
    ) = initializeGitRepo(
        directory = projectDir,
        remoteUrl = remoteUrl,
        addFileNames = addFileNames,
        initialTag = initialTag,
        commits = commits,
    )

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

    fun execute(): TestResult

    @Test
    fun withNoTagsProducesError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"))
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Failure ->
                assertContains(
                    result.reason,
                    Regex("Inappropriate configuration: repository has no tags.\\s*\n\\s*If this is a new repository, use `tag` to set the initial version."),
                )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun whenNoRemoteProduceError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf("init", "commit (no) 1"),
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "Inappropriate configuration: repository has no remote.",
            )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun whenNoRemoteButDisableDetachedIsFalseDoNotError() = setup(object {
    }) {
        configureWithOverrides(disableDetached = false)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf("init", "commit (no) 1"),
            initialTag = "1.2.3",
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun whenCurrentCommitAlreadyHasTagWillUseTag() = setup(object {
        val gitAdapter = GitAdapter(
            projectDir,
            mapOf(
                "PATH" to (getEnvironmentVariable("PATH") ?: ""),
                "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
                "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
            ),
        )
    }) {
        configureWithDefaults()
        gitAdapter.init()
        gitAdapter.config("commit.gpgsign", "false")
        gitAdapter.add(".")
        gitAdapter.addCommitWithMessage("test commit")
        gitAdapter.newAnnotatedTag("1.0.23", "HEAD", "test", "test")
        val currentBranch = gitAdapter.status().head
        if (currentBranch == "main") {
            gitAdapter.checkout("main")
        } else {
            gitAdapter.checkout("main", newBranch = true)
        }
        gitAdapter.addRemote(name = "origin", url = projectDir)
        gitAdapter.fetch()
        gitAdapter.setBranchUpstream("origin/main", "main")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.0.23-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun whenPreviousTagDoesNotHaveThreeNumbersWillError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "Inappropriate configuration: the most recent tag did not have all three semver components.",
            )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun withAllPatchCommitsOnlyIncrementsPatch() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun givenNoImplicitPatchCalculatingVersionWithUnlabeledCommitsDoesNotIncrement() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = false)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.3-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun givenInitialTagWithSuffixIgnoreSuffixAndFollowNormalRules() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = false)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3-SNAPSHOT")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.3-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithUnlabeledCommitsIncrementsPatch() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneAndThenUnlabeledCommitsIncrementsPatch() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "[none] commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneCommitsDoesNotIncrementAndIsAlwaysSnapshot() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "[None] commit 1", "[none] commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.3-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun withOneMinorCommitsOnlyIncrementsMinor() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[minor] commit 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithDefaults()
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.3.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun canReplaceMajorRegex() = setup(object {
        val majorRegex = ".*(big).*"
        val commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, majorRegex = majorRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("2.0.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("2.0.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.3.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun canReplaceMinorRegex() = setup(object {
        val minorRegex = ".*(middle).*"
        val commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, minorRegex = minorRegex)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.3.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun canReplacePatchRegex() = setup(object {
        val patchRegex = ".*(tiny).*"
        val commits = listOf("init", "commit 1", "commit (tiny) 2", "commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, patchRegex = patchRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "commit 1", "commit (widdle) 2", "commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun canReplaceNoneRegex() = setup(object {
        val noneRegex = ".*(no).*"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = true,
            noneRegex = noneRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.3-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = true,
            versionRegex = versionRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.3-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun calculatingVersionWithOneMajorCommitsOnlyIncrementsMajor() = setup(object {
        val commits = listOf("init", "[major] commit 1", "[minor] commit 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithDefaults()
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("2.0.0", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() = setup(object {
        val versionRegex = ".*"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = true, versionRegex = versionRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "version regex must include groups named 'major', 'minor', 'patch', and 'none'.",
            )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun forceSnapshotMakesReleaseVersionsBecomeSnapshots() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(forceSnapshot = true)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> assertEquals("1.2.4-SNAPSHOT", result.message)
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }

    @Test
    fun forceSnapshotReportsForcedReason() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(forceSnapshot = true)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        when (result) {
            is TestResult.Success -> {
                assertEquals("1.2.4-SNAPSHOT", result.message)
                assertTrue(
                    actual = result.details.contains("FORCED"),
                    message = "Expected snapshot reason output to include FORCED. Details:\n${result.details}",
                )
            }

            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }
    }
}
