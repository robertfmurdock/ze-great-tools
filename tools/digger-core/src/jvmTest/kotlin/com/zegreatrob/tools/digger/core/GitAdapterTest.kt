package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.addCommitWithMessage
import com.zegreatrob.tools.digger.addTag
import com.zegreatrob.tools.digger.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.wrapper.git.GitAdapter
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

    fun initializeGitRepo(commits: List<String>) = com.zegreatrob.tools.digger.initializeGitRepo(
        projectDirectoryPath = projectDir.absolutePath,
        addFileNames = emptySet(),
        commits = commits,
    )

    @Test
    fun `will include all tag segments from newest to oldest`() {
        val grgit = initializeGitRepo(listOf("here's a message"))
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
