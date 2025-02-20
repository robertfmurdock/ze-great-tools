package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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
        majorRegex: String? = null,
        minorRegex: String? = null,
        patchRegex: String? = null,
        versionRegex: String? = null,
        noneRegex: String? = null,
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

    fun runCalculateVersionSuccessfully(): String = when (val result = execute()) {
        is TestResult.Success -> result.message
        is TestResult.Failure -> fail("Expected success but got ${result.reason}")
    }

    @Test
    fun withNoTagsProducesError() {
        configureWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"))
        when (val result = execute()) {
            is TestResult.Failure ->
                assertContains(
                    result.reason,
                    Regex("Inappropriate configuration: repository has no tags.\\s*\n\\s*If this is a new repository, use `tag` to set the initial version."),
                )
            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun whenNoRemoteProduceError() {
        configureWithDefaults()

        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf<String>("init", "commit (no) 1"),
        )
        when (val result = execute()) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "Inappropriate configuration: repository has no remote.",
            )
            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }

    @Test
    fun whenCurrentCommitAlreadyHasTagWillUseTag() {
        configureWithDefaults()

        val gitAdapter = GitAdapter(
            projectDir,
            mapOf(
                "PATH" to (getEnvironmentVariable("PATH") ?: ""),
                "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
                "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
            ),
        )
        gitAdapter.init()
        gitAdapter.config("commit.gpgsign", "false")
        gitAdapter.add(".")
        gitAdapter.addCommitWithMessage("test commit")
        gitAdapter.newAnnotatedTag("1.0.23", "HEAD", "test", "test")
        gitAdapter.checkout("main", newBranch = true)
        gitAdapter.addRemote(name = "origin", url = projectDir)
        gitAdapter.fetch()
        gitAdapter.setBranchUpstream("origin/main", "main")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.0.23-SNAPSHOT", version)
    }

    @Test
    fun withAllPatchCommitsOnlyIncrementsPatch() {
        configureWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun givenNoImplicitPatchCalculatingVersionWithUnlabeledCommitsDoesNotIncrement() {
        configureWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun givenInitialTagWithSuffixIgnoreSuffixAndFollowNormalRules() {
        configureWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3-SNAPSHOT")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithUnlabeledCommitsIncrementsPatch() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneAndThenUnlabeledCommitsIncrementsPatch() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[none] commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneCommitsDoesNotIncrementAndIsAlwaysSnapshot() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[None] commit 1", "[none] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun withOneMinorCommitsOnlyIncrementsMinor() {
        configureWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMajorRegex() {
        configureWithOverrides(implicitPatch = false, majorRegex = ".*(big).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )

        val version = runCalculateVersionSuccessfully()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMinorRegex() {
        configureWithOverrides(implicitPatch = false, minorRegex = ".*(middle).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplacePatchRegex() {
        configureWithOverrides(implicitPatch = false, patchRegex = ".*(tiny).*")

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (tiny) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (widdle) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun canReplaceNoneRegex() {
        configureWithOverrides(
            implicitPatch = true,
            noneRegex = ".*(no).*",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() {
        configureWithOverrides(
            implicitPatch = true,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun calculatingVersionWithOneMajorCommitsOnlyIncrementsMajor() {
        configureWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[major] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()
        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() {
        configureWithOverrides(implicitPatch = true, versionRegex = ".*")

        initializeGitRepo(listOf("init", "commit (no) 1"), "1.2.3")
        when (val result = execute()) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "version regex must include groups named 'major', 'minor', 'patch', and 'none'.",
            )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }
}
