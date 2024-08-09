package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.addCommitWithMessage
import com.zegreatrob.tools.digger.addTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class DiggerGitWrapperTest {
    @field:TempDir
    lateinit var projectDir: File
    private lateinit var wrapper: DiggerGitWrapper

    @BeforeTest
    fun setup() {
        wrapper = DiggerGitWrapper(projectDir.absolutePath)
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

    private fun delayLongEnoughToAffectGitDate() {
        Thread.sleep(1000)
    }
}
