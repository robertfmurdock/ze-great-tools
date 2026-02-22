package com.zegreatrob.tools.adapter.git

import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class GitAdapterTest {

    @field:TempDir
    lateinit var projectDir: File

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
        assertEquals(listOf(newestTag, newerTag, initialTag), result.map { it.name })
    }
}
