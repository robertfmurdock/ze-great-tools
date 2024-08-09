package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.addCommitWithMessage
import com.zegreatrob.tools.digger.addTag
import com.zegreatrob.tools.digger.initializeGitRepo
import com.zegreatrob.tools.digger.mergeInBranch
import com.zegreatrob.tools.digger.switchToNewBranch
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AllPathsTest {

    @field:TempDir
    lateinit var projectDir: File

    private val diggerGitWrapper by lazy { DiggerGitWrapper(projectDir.absolutePath) }

    @Test
    fun willFindAllPaths() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()

        grgit.addTag("release")
        grgit.switchToNewBranch("branch1")

        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }

        val thirdCommit = grgit.addCommitWithMessage("third")
        grgit.addTag("release2")
        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit = grgit.mergeInBranch("branch1", "merge")
        grgit.addTag("release3")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit.toCommitRef())

        assertEquals(
            setOf(
                listOf(mergeCommit, thirdCommit, firstCommit).map { it.toCommitRef() },
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit).map { it.toCommitRef() },
            ).justIds(),
            allPaths.toSet().justIds(),
        )
    }

    private fun Set<List<CommitRef>>.justIds() = map { it.map(CommitRef::id) }.toSet()

    private fun Commit.toCommitRef() = CommitRef(
        id = id,
        authorEmail = author.email,
        committerEmail = committer.email,
        dateTime = dateTime.toInstant().toKotlinInstant(),
        parents = parentIds.toList(),
        fullMessage = fullMessage,
    )
}
