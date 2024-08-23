package com.zegreatrob.tools.adapter.git

import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
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
        val grgit = initializeGitRepo(commits = listOf("here's a message"))
        grgit.addTag("v1.0")
        delayLongEnoughToAffectGitDate()
        grgit.addCommitWithMessage("here's a message")
        grgit.addTag("1.10")
        delayLongEnoughToAffectGitDate()
        grgit.addCommitWithMessage("here's a message")
        grgit.addTag("1.101")

        assertEquals(listOf("1.101", "1.10", "v1.0"), wrapper.listTags().map { it.name })
    }
}
