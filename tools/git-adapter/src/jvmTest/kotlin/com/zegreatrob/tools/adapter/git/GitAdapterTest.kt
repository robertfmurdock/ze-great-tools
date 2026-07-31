package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class GitAdapterTest {

    @field:TempDir
    lateinit var projectDir: File

    @Test
    fun `commandLogger callback receives git command when provided`() = asyncSetup(object {
        val loggedCommands = mutableListOf<String>()
        val commandLogger: (String) -> Unit = { loggedCommands.add(it) }
        val commitMessage = "initial commit"
        val wrapper = initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf(commitMessage),
        ).let { GitAdapter(projectDir.absolutePath, commandLogger = commandLogger) }
    }) exercise {
        wrapper.headCommitId()
    } verify {
        loggedCommands.size.assertIsEqualTo(1)
        loggedCommands.first().assertIsEqualTo("git --no-pager rev-parse HEAD")
    }

    @Test
    fun `commandLogger callback is not invoked when null`() = asyncSetup(object {
        val commitMessage = "initial commit"
        val wrapper = initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf(commitMessage),
        ).let { GitAdapter(projectDir.absolutePath, commandLogger = null) }
    }) exercise {
        wrapper.headCommitId()
    } verify {
        // Should complete without error when callback is null
    }

    @Test
    fun `will include all tag segments from newest to oldest`() = asyncSetup(object {
        val wrapper = GitAdapter(projectDir.absolutePath)
        val initialTag = "v1.0"
        val newerTag = "1.10"
        val newestTag = "1.101"
        val commitMessage = "here's a message"
    }) {
        initializeGitRepo(
            directory = projectDir.absolutePath,
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
