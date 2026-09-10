package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GitAdapterTest {

    private lateinit var projectDir: String

    @BeforeTest
    fun setup() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun teardown() {
        removeDirectory(projectDir)
    }

    @Test
    fun commandLoggerCallbackReceivesGitCommandWhenProvided() = asyncSetup(object {
        val loggedCommands = mutableListOf<String>()
        val commandLogger: (String) -> Unit = { loggedCommands.add(it) }
        val commitMessage = "initial commit"
        val wrapper = initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf(commitMessage),
        ).let { GitAdapter(projectDir, commandLogger = commandLogger) }
    }) exercise {
        wrapper.headCommitId()
    } verify {
        loggedCommands.size.assertIsEqualTo(1)
        loggedCommands.first().assertIsEqualTo("git --no-pager rev-parse HEAD")
    }

    @Test
    fun commandLoggerCallbackIsNotInvokedWhenNull() = asyncSetup(object {
        val commitMessage = "initial commit"
        val wrapper = initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf(commitMessage),
        ).let { GitAdapter(projectDir, commandLogger = null) }
    }) exercise {
        wrapper.headCommitId()
    } verify {
    }

    @Test
    fun willIncludeAllTagSegmentsFromNewestToOldest() = asyncSetup(object {
        val wrapper = GitAdapter(projectDir)
        val initialTag = "v1.0"
        val newerTag = "1.10"
        val newestTag = "1.101"
        val commitMessage = "here's a message"
    }) {
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf(commitMessage),
        ).apply {
            config("user.name", "Test")
            config("user.email", "Test")
            newAnnotatedTag(initialTag, "HEAD", null, null)
        }
        delayLongEnoughToAffectGitDate()
        wrapper.addCommitWithMessage(commitMessage)
        wrapper.newAnnotatedTag(newerTag, "HEAD", null, null)
        delayLongEnoughToAffectGitDate()
        wrapper.addCommitWithMessage(commitMessage)
        wrapper.newAnnotatedTag(newestTag, "HEAD", null, null)
    } exercise {
        wrapper.listTags()
    } verify { result ->
        result.map { it.name }.assertIsEqualTo(listOf(newestTag, newerTag, initialTag))
    }
}
