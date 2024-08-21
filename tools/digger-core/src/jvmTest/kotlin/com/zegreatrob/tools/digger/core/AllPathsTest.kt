package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.addCommitWithMessage
import com.zegreatrob.tools.digger.initializeGitRepo
import com.zegreatrob.tools.digger.mergeInBranch
import com.zegreatrob.tools.digger.switchToNewBranch
import com.zegreatrob.tools.wrapper.git.CommitRef
import com.zegreatrob.tools.wrapper.git.GitAdapter
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AllPathsTest {

    @field:TempDir
    lateinit var projectDir: File

    private val diggerGitWrapper by lazy { GitAdapter(projectDir.absolutePath) }

    @Test
    fun willHandleSimplePathsEasily() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch1")

        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }

        val thirdCommit = grgit.addCommitWithMessage("third")
        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit = grgit.mergeInBranch("branch1", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit.toCommitRef())

        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, thirdCommit, firstCommit).map { it.toCommitRef() },
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    @Test
    fun willIgnoreBranchesWhenOneParentIsInPreferredList() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch1")

        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }

        grgit.addCommitWithMessage("third")
        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit = grgit.mergeInBranch("branch1", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit.toCommitRef(), preferredCommitIds = setOf(fourthCommit.id))

        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    @Test
    fun branchMergeBranchMergeFindsAllPaths() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch1")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch1" }
        val fourthCommit = grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit1 = grgit.mergeInBranch("branch1", "merge")
        val fifthCommit = grgit.addCommitWithMessage("fifth")

        grgit.switchToNewBranch("branch2")
        val sixthCommit = grgit.addCommitWithMessage("sixth")
        grgit.checkout { it.branch = "main" }
        val seventhCommit = grgit.addCommitWithMessage("seventh")
        val mergeCommit2 = grgit.mergeInBranch("branch2", "merge")
        val eighthCommit = grgit.addCommitWithMessage("eighth")

        grgit.switchToNewBranch("branch3")
        val ninthCommit = grgit.addCommitWithMessage("ninth")
        grgit.checkout { it.branch = "main" }
        val tenthCommit = grgit.addCommitWithMessage("tenth")
        val mergeCommit3 = grgit.mergeInBranch("branch3", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit3.toCommitRef())

        assertExpectedPaths(
            setOf(
                listOf(
                    mergeCommit3,
                    tenthCommit,
                    eighthCommit,
                    mergeCommit2,
                    seventhCommit,
                    fifthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    ninthCommit,
                    eighthCommit,
                    mergeCommit2,
                    seventhCommit,
                    fifthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    tenthCommit,
                    eighthCommit,
                    mergeCommit2,
                    seventhCommit,
                    fifthCommit,
                    mergeCommit1,
                    fourthCommit,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    ninthCommit,
                    eighthCommit,
                    mergeCommit2,
                    seventhCommit,
                    fifthCommit,
                    mergeCommit1,
                    fourthCommit,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    tenthCommit,
                    eighthCommit,
                    mergeCommit2,
                    sixthCommit,
                    fifthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    ninthCommit,
                    eighthCommit,
                    mergeCommit2,
                    sixthCommit,
                    fifthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    tenthCommit,
                    eighthCommit,
                    mergeCommit2,
                    sixthCommit,
                    fifthCommit,
                    mergeCommit1,
                    fourthCommit,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    mergeCommit3,
                    ninthCommit,
                    eighthCommit,
                    mergeCommit2,
                    sixthCommit,
                    fifthCommit,
                    mergeCommit1,
                    fourthCommit,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    @Test
    fun willStopOnceFindingPathContainingAllPreferredCommits() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch1")
        grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "main" }
        val mergeCommit1 = grgit.mergeInBranch("branch1", "merge")
        val fifthCommit = grgit.addCommitWithMessage("fifth")

        grgit.switchToNewBranch("branch2")
        val sixthCommit = grgit.addCommitWithMessage("sixth")
        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("seventh")
        val mergeCommit2 = grgit.mergeInBranch("branch2", "merge")
        val eighthCommit = grgit.addCommitWithMessage("eighth")

        grgit.switchToNewBranch("branch3")
        grgit.addCommitWithMessage("ninth")
        grgit.checkout { it.branch = "main" }
        val tenthCommit = grgit.addCommitWithMessage("tenth")
        val mergeCommit3 = grgit.mergeInBranch("branch3", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(
            log,
            mergeCommit3.toCommitRef(),
            preferredCommitIds = setOf(tenthCommit.id, sixthCommit.id, thirdCommit.id),
        )

        assertExpectedPaths(
            setOf(
                listOf(
                    mergeCommit3,
                    tenthCommit,
                    eighthCommit,
                    mergeCommit2,
                    sixthCommit,
                    fifthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    @Test
    fun mergeToBranchAndBackFindsAllPaths() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch" }
        val mergeCommit1 = grgit.mergeInBranch("main", "merge")
        val fourthCommit = grgit.addCommitWithMessage("fourth")

        grgit.checkout { it.branch = "main" }
        val singleParentMerge = grgit.mergeInBranch("branch", "merge")
        val fifthCommit = grgit.addCommitWithMessage("fifth")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, fifthCommit.toCommitRef())

        assertExpectedPaths(
            setOf(
                listOf(
                    fifthCommit,
                    singleParentMerge,
                    fourthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    fifthCommit,
                    singleParentMerge,
                    fourthCommit,
                    mergeCommit1,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    @Test
    fun branchOnBranchOnBranchFindsAllPaths() {
        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = grgit.head()
        grgit.switchToNewBranch("branch1")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.checkout { it.branch = "main" }
        val thirdCommit = grgit.addCommitWithMessage("third")

        grgit.switchToNewBranch("branch2")
        val fourthCommit = grgit.addCommitWithMessage("forth")

        grgit.checkout { it.branch = "main" }
        val fifthCommit = grgit.addCommitWithMessage("fifth")

        grgit.checkout { it.branch = "branch1" }
        val sixthCommit = grgit.addCommitWithMessage("sixth")

        grgit.checkout { it.branch = "main" }
        val seventhCommit = grgit.addCommitWithMessage("seventh")

        grgit.checkout { it.branch = "branch2" }
        val eighthCommit = grgit.addCommitWithMessage("eighth")
        val merge1 = grgit.mergeInBranch("branch1", "merge")
        val ninthCommit = grgit.addCommitWithMessage("eighth")

        grgit.checkout { it.branch = "main" }
        val merge2 = grgit.mergeInBranch("branch2", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, merge2.toCommitRef())

        assertExpectedPaths(
            setOf(
                listOf(
                    merge2,
                    seventhCommit,
                    fifthCommit,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    merge2,
                    ninthCommit,
                    merge1,
                    eighthCommit,
                    fourthCommit,
                    thirdCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
                listOf(
                    merge2,
                    ninthCommit,
                    merge1,
                    sixthCommit,
                    secondCommit,
                    firstCommit,
                ).map { it.toCommitRef() },
            ),
            allPaths,
        )
    }

    private fun assertExpectedPaths(
        expected: Set<List<CommitRef>>,
        allPaths: MutableList<List<CommitRef>>,
    ) {
        assertEquals(
            expected.justIds(),
            allPaths.toSet().justIds(),
            errorMessage(expected, allPaths.toSet()),
        )
    }

    private fun errorMessage(
        expected: Set<List<CommitRef>>,
        allPaths: Set<List<CommitRef>>,
    ): String = "expected but did not find: ${(expected - allPaths).justIds().joinToString(",")}\n" +
        "actual had extra: ${(allPaths - expected).justIds().joinToString(",")}\n"

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
