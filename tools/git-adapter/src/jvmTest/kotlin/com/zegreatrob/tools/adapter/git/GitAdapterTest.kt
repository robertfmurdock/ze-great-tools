package com.zegreatrob.tools.adapter.git

import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class GitAdapterTest {

    @field:TempDir
    lateinit var projectDir: File
    private lateinit var wrapper: GitAdapter

    @BeforeTest
    fun setup() {
        wrapper = GitAdapter(projectDir.absolutePath)
    }

    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        directory = projectDir.absolutePath,
        addFileNames = emptySet(),
        commits = commits,
    )

    @Test
    fun `will include all tag segments from newest to oldest`() {
        val gitAdapter = initializeGitRepo(commits = listOf("here's a message"))
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")
        gitAdapter.newAnnotatedTag("v1.0", "HEAD", null, null)
        delayLongEnoughToAffectGitDate()
        gitAdapter.addCommitWithMessage("here's a message")
        gitAdapter.newAnnotatedTag("1.10", "HEAD", null, null)
        delayLongEnoughToAffectGitDate()
        gitAdapter.addCommitWithMessage("here's a message")
        gitAdapter.newAnnotatedTag("1.101", "HEAD", null, null)

        assertEquals(listOf("1.101", "1.10", "v1.0"), wrapper.listTags().map { it.name })
    }
}
