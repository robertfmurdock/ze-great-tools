package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.runProcess
import com.zegreatrob.tools.tagger.core.TagErrors
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.InitOp
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot

interface TagTestSpec {
    var projectDir: File
    val addFileNames: Set<String>

    fun initializeGitRepo(
        commits: List<String>,
        initialTag: String? = null,
        remoteUrl: String = projectDir.absolutePath,
    ) = com.zegreatrob.tools.test.git.initializeGitRepo(
        directory = projectDir.absolutePath,
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

    @BeforeTest
    fun checkPrerequisites() {
        assertEquals(
            "/dev/null",
            System.getenv("GIT_CONFIG_GLOBAL"),
            "Ensure this is set for the test to work as intended",
        )
        assertEquals(
            "/dev/null",
            System.getenv("GIT_CONFIG_SYSTEM"),
            "Ensure this is set for the test to work as intended",
        )
    }

    @Test
    fun whenUserNameAndEmailAreConfiguredTagWillTagAndPush() {
        configureWithDefaults()

        val originDirectory = createTempDirectory()
        val originGitAdapter = GitAdapter(originDirectory.absolutePathString())
        originGitAdapter.init()
        val originGrgit = Grgit.open(mapOf("dir" to originDirectory.absolutePathString()))
        originGitAdapter.disableGpgSign()

        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), this.projectDir.absolutePath)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), this.projectDir.absolutePath)

        val expectedVersion = "1.0.0"
        val result = execute(expectedVersion)
        assertIsNot<TestResult.Failure>(result, message = "$result")

        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertEquals(expectedVersion, gitAdapter.showTag("HEAD"))
    }

    fun GitAdapter.disableGpgSign() {
        config("commit.gpgsign", "false")
    }

    @Test
    fun whenUserNameAndEmailAreParametersTagWillTagAndPush() {
        configureWithOverrides(
            releaseBranch = "master",
            userName = "RoB as Test",
            userEmail = "test@zegreatrob.com",
            warningsAsErrors = true,
        )

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        val originGitAdapter = GitAdapter(originDirectory.absolutePathString())
        originGitAdapter.disableGpgSign()
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        val expectedVersion = "1.0.0"
        val result = execute(expectedVersion)
        assertIsNot<TestResult.Failure>(result, message = "$result")

        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertEquals(expectedVersion, gitAdapter.showTag("HEAD"))
    }

    @Test
    fun tagWillFailWhenUserEmailAndNameAreNotConfigured() {
        configureWithOverrides(releaseBranch = "master", warningsAsErrors = true)

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        val originGitAdapter = GitAdapter(originDirectory.absolutePathString())
        originGitAdapter.disableGpgSign()
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        val version = "1.0.0"
        val result = execute(version)
        assertContains(
            charSequence = assertIs<TestResult.Failure>(result).reason,
            other = "Committer identity unknown",
        )
        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertNotEquals(version, gitAdapter.showTag("HEAD"))
    }

    @Test
    fun whenNotOnCorrectBranchAndWarningsAsErrorsTagWillNotDoAnythingAndError() {
        configureWithOverrides(releaseBranch = "trunk", warningsAsErrors = true)

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        val originGitAdapter = GitAdapter(originDirectory.absolutePathString())
        originGitAdapter.disableGpgSign()
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), this.projectDir.absolutePath)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), this.projectDir.absolutePath)

        val version = "1.0.0"
        val result = execute(version)
        assertContains(
            charSequence = assertIs<TestResult.Failure>(result).reason,
            other = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master")),
        )
        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertNotEquals(version, gitAdapter.showTag("HEAD"))
    }

    @Test
    fun whenNotOnCorrectBranchTagWillNotDoAnythingAndError() {
        configureWithOverrides(releaseBranch = "trunk", warningsAsErrors = false)

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        val originGitAdapter = GitAdapter(originDirectory.absolutePathString())
        originGitAdapter.disableGpgSign()
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), this.projectDir.absolutePath)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), this.projectDir.absolutePath)

        val version = "1.0.0"
        val result = execute(version)
        assertContains(
            charSequence = assertIs<TestResult.Success>(result).message,
            other = TagErrors.wrapper(TagErrors.skipMessageNotOnReleaseBranch("trunk", "master")),
        )
        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertNotEquals(version, gitAdapter.showTag("HEAD"))
    }
}
