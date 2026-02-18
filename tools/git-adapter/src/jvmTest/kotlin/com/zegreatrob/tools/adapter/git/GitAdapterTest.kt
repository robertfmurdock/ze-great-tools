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
    }) {
        initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf("here's a message"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "Test")
            newAnnotatedTag("v1.0", "HEAD", null, null)
        }
        delayLongEnoughToAffectGitDate()
        wrapper.addCommitWithMessage("here's a message")
        wrapper.newAnnotatedTag("1.10", "HEAD", null, null)
        delayLongEnoughToAffectGitDate()
        wrapper.addCommitWithMessage("here's a message")
        wrapper.newAnnotatedTag("1.101", "HEAD", null, null)
    } exercise {
        wrapper.listTags()
    } verify { result ->
        assertEquals(listOf("1.101", "1.10", "v1.0"), result.map { it.name })
    }
}
