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

    fun createOrigin(env: Map<String, String> = emptyMap()): Pair<String, GitAdapter> {
        val directory = createTempDirectory()
        val adapter = GitAdapter(directory, env)
        adapter.init()
        adapter.config("receive.denyCurrentBranch", "ignore")
        adapter.disableGpgSign()
        adapter.addCommitWithMessage("init")
        return directory to adapter
    }

    fun initializeIdentitylessRepo(): GitAdapter {
        val environment = listOf("PATH", "GIT_CONFIG_GLOBAL", "GIT_CONFIG_SYSTEM")
            .associateWith { getEnvironmentVariable(it) ?: "" }
        val (originDirectory) = createOrigin(environment)
        return initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory,
        ).also(GitAdapter::push)
    }

    fun initializeRepoWithUnavailableRemote(
        commits: List<String>,
        initialTag: String? = null,
    ): Pair<GitAdapter, GitAdapter> {
        val (originDirectory, originGitAdapter) = createOrigin()
        val gitAdapter = initializeGitRepo(commits, initialTag, originDirectory)
        gitAdapter.push()
        gitAdapter.config("remote.origin.url", "$originDirectory/unavailable")
        return gitAdapter to originGitAdapter
    }

    fun configureWithDefaults()
    fun configureWithOverrides(
        releaseBranch: String? = null,
        userName: String? = null,
        userEmail: String? = null,
        warningsAsErrors: Boolean? = null,
        allowDetachedHead: Boolean? = null,
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
        result.assertIsOfType<TestResult.Success>()
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
        result.assertIsOfType<TestResult.Success>()
        gitAdapter.showTag("HEAD")?.name.assertIsEqualTo(expectedVersion)
    }

    @Test
    fun tagWillFailWhenUserEmailAndNameAreNotConfigured() = setup(object {
        val version = "1.0.0"
        val expectedError = "Committer identity unknown"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = true)

        gitAdapter = initializeIdentitylessRepo()
    } exercise {
        execute(version)
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(expectedError).assertIsEqualTo(
                true,
                "Expected error to include: $expectedError\nActual:\n$reason",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }

    @Test
    fun tagWillFailWhenUserEmailAndNameAreNotConfiguredAndWarningsAreNotErrors() = setup(object {
        val version = "1.0.0"
        val expectedError = "Committer identity unknown"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = false)

        gitAdapter = initializeIdentitylessRepo()
    } exercise {
        execute(version)
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(expectedError).assertIsEqualTo(
                true,
                "Expected error to include: $expectedError\nActual:\n$reason",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
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
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(expectedError).assertIsEqualTo(
                true,
                "Expected error to include: $expectedError\nActual:\n$reason",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
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
        result.assertIsOfType<TestResult.Success>().run {
            message.contains(expectedMessage).assertIsEqualTo(
                true,
                "Expected message to include: $expectedMessage\nActual:\n$message",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }

    @Test
    fun whenAllowDetachedHeadTrueButNotDetachedAndNotOnReleaseBranchWillError() = setup(object {
        val version = "1.0.0"
        val expectedError = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master"))
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(
            releaseBranch = "trunk",
            warningsAsErrors = true,
            allowDetachedHead = true,
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

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)
    } exercise {
        execute(version)
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(expectedError).assertIsEqualTo(
                true,
                "Expected error to include: $expectedError\nActual:\n$reason",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsNotEqualTo(version)
    }

    @Test
    fun allowDetachedHeadPermitsTaggingDetachedHead() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
        val expectedVersion = "1.2.4"
    }) {
        configureWithOverrides(
            releaseBranch = "master",
            allowDetachedHead = true,
        )

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")

        val gitAdapter = initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
            remoteUrl = originDirectory,
        )
        gitAdapter.push()

        runProcess(listOf("git", "checkout", "--detach"), projectDir)
        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)
    } exercise {
        execute(expectedVersion)
    } verify { result ->
        result.assertIsOfType<TestResult.Success>()

        val gitAdapter = GitAdapter(projectDir)
        gitAdapter.showTag("HEAD")
            ?.name
            .assertIsEqualTo(expectedVersion)
    }

    @Test
    fun whenTagAlreadyExistsOnCurrentCommitTagWillSucceedIdempotently() = setup(object {
        val expectedVersion = "1.0.0"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = false)

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

        val firstResult = execute(expectedVersion)
        firstResult.assertIsOfType<TestResult.Success>()
    } exercise {
        execute(expectedVersion)
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().run {
            message.contains(TagErrors.alreadyTagged(expectedVersion))
                .assertIsEqualTo(false, "Expected no warning when tag exists on same commit for idempotency")
        }
        gitAdapter.showTag("HEAD")?.name.assertIsEqualTo(expectedVersion)
    }

    @Test
    fun tagWillFailWhenTagsCannotBePushedAndWarningsAreNotErrors() = setup(object {
        val version = "1.0.0"
        val expectedError = "Command failed: git push --tags"
        lateinit var gitAdapter: GitAdapter
        lateinit var originGitAdapter: GitAdapter
    }) {
        configureWithOverrides(
            releaseBranch = "master",
            userName = "RoB as Test",
            userEmail = "test@zegreatrob.com",
            warningsAsErrors = false,
        )

        val adapters = initializeRepoWithUnavailableRemote(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
        )
        gitAdapter = adapters.first
        originGitAdapter = adapters.second
    } exercise {
        execute(version)
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(expectedError).assertIsEqualTo(
                true,
                "Expected error to include: $expectedError\nActual:\n$reason",
            )
        }
        gitAdapter.showTag("HEAD")?.name.assertIsEqualTo(version)
        originGitAdapter.listAllTagNames().contains(version).assertIsEqualTo(false)
    }

    @Test
    fun whenTagExistsOnDifferentCommitTagWillFailWithError() = setup(object {
        val version = "1.0.0"
        lateinit var gitAdapter: GitAdapter
    }) {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = true)

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.disableGpgSign()
        originGitAdapter.addCommitWithMessage("init")
        gitAdapter = initializeGitRepo(
            listOf("init", "[patch] commit 1"),
            remoteUrl = originDirectory,
        )
        gitAdapter.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)

        execute(version)

        gitAdapter.addCommitWithMessage("[patch] commit 2")
    } exercise {
        execute(version)
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("already exists").assertIsEqualTo(
                true,
                "Expected error to mention tag already exists\nActual:\n$reason",
            )
        }
    }
}
