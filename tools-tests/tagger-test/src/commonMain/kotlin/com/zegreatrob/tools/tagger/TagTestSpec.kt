package com.zegreatrob.tools.tagger

import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.runProcess
import com.zegreatrob.tools.tagger.core.TagErrors
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
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals

interface TagTestSpec {
    private fun test(block: () -> Unit) = setup(object {}) exercise { block() } verify { }

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

    fun configureWithDefaults()
    fun configureWithOverrides(
        releaseBranch: String? = null,
        userName: String? = null,
        userEmail: String? = null,
        warningsAsErrors: Boolean? = null,
    )

    fun execute(version: String): TestResult

    @Test
    fun whenUserNameAndEmailAreConfiguredTagWillTagAndPush() = setup(object {
        val expectedVersion = "1.0.0"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithDefaults()

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)
    } exercise {
        execute(expectedVersion)
    } verify { result ->
        assertIsNot<TestResult.Failure>(result, message = "$result")
        assertEquals(expectedVersion, gitAdapter.showTag("HEAD")?.name)
    }

    fun GitAdapter.disableGpgSign() {
        config("commit.gpgsign", "false")
    }

    @Test
    fun whenUserNameAndEmailAreParametersTagWillTagAndPush() = setup(object {
        val expectedVersion = "1.0.0"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(
            releaseBranch = "master",
            userName = "RoB as Test",
            userEmail = "test@zegreatrob.com",
            warningsAsErrors = true,
        )

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()
    } exercise {
        execute(expectedVersion)
    } verify { result ->
        assertIsNot<TestResult.Failure>(result, message = "$result")
        assertEquals(expectedVersion, gitAdapter.showTag("HEAD")?.name)
    }

    @Test
    fun tagWillFailWhenUserEmailAndNameAreNotConfigured() = setup(object {
        val version = "1.0.0"
        val expectedError = "Committer identity unknown"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = true)

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(
            originDirectory,
            mapOf(
                "PATH" to (getEnvironmentVariable("PATH") ?: ""),
                "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
                "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
            ),
        )
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()
    } exercise {
        execute(version)
    } verify { result ->
        assertContains(
            charSequence = assertIs<TestResult.Failure>(result).reason,
            other = expectedError,
        )
        assertNotEquals(version, gitAdapter.showTag("HEAD")?.name)
    }

    @Test
    fun whenNotOnCorrectBranchAndWarningsAsErrorsTagWillNotDoAnythingAndError() = setup(object {
        val version = "1.0.0"
        val expectedError = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master"))
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "trunk", warningsAsErrors = true)

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)
    } exercise {
        execute(version)
    } verify { result ->
        assertContains(
            charSequence = assertIs<TestResult.Failure>(result).reason,
            other = expectedError,
        )
        assertNotEquals(version, gitAdapter.showTag("HEAD")?.name)
    }

    @Test
    fun whenNotOnCorrectBranchTagWillNotDoAnythingAndError() = setup(object {
        val version = "1.0.0"
        val expectedMessage = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master"))
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "trunk", warningsAsErrors = false)

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)
    } exercise {
        execute(version)
    } verify { result ->
        assertContains(
            charSequence = assertIs<TestResult.Success>(result).message,
            other = expectedMessage,
        )
        assertNotEquals(version, gitAdapter.showTag("HEAD")?.name)
    }
}
