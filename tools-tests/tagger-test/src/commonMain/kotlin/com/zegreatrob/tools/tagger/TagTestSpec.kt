package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
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

interface TagTestSpec {
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
        getEnvironmentVariable("GIT_CONFIG_GLOBAL").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
        getEnvironmentVariable("GIT_CONFIG_SYSTEM").assertIsEqualTo(
            "/dev/null",
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
        (result is TestResult.Failure).assertIsEqualTo(false, "$result")
        gitAdapter.showTag("HEAD")?.name.assertIsEqualTo(expectedVersion)
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
        (result is TestResult.Failure).assertIsEqualTo(false, "$result")
        gitAdapter.showTag("HEAD")?.name.assertIsEqualTo(expectedVersion)
    }

    @Test
    fun tagWillFailWhenUserEmailAndNameAreNotConfigured() = setup(object {
        val version = "1.0.0"
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
        val failure = result as? TestResult.Failure
        val expectedError = "Committer identity unknown"
        failure.assertIsNotEqualTo(null, "Expected failure result.")
        failure?.reason?.contains(expectedError).assertIsEqualTo(
            true,
            "Expected error to include: $expectedError\nActual:\n${failure?.reason}",
        )
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }

    @Test
    fun whenNotOnCorrectBranchAndWarningsAsErrorsTagWillNotDoAnythingAndError() = setup(object {
        val version = "1.0.0"
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
        val failure = result as? TestResult.Failure
        val expectedError = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master"))
        failure.assertIsNotEqualTo(null, "Expected failure result.")
        failure?.reason?.contains(expectedError).assertIsEqualTo(
            true,
            "Expected error to include: $expectedError\nActual:\n${failure?.reason}",
        )
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }

    @Test
    fun whenNotOnCorrectBranchTagWillNotDoAnythingAndError() = setup(object {
        val version = "1.0.0"
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
        val success = result as? TestResult.Success
        val expectedMessage = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master"))
        success.assertIsNotEqualTo(null, "Expected success result.")
        success?.message?.contains(expectedMessage).assertIsEqualTo(
            true,
            "Expected message to include: $expectedMessage\nActual:\n${success?.message}",
        )
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }
}
