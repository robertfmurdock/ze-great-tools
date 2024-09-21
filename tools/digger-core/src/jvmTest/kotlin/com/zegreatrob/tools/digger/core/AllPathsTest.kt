package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.switchToNewBranch
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
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch1")

        val secondCommit = gitAdapter.addCommitWithMessage("second")
        gitAdapter.checkout("master")

        val thirdCommit = gitAdapter.addCommitWithMessage("third")
        gitAdapter.checkout("branch1")
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit)

        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, thirdCommit, firstCommit),
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit),
            ),
            allPaths,
        )
    }

    @Test
    fun willIgnoreBranchesWhenOneParentIsInPreferredList() {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")
        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch1")

        val secondCommit = gitAdapter.addCommitWithMessage("second")
        gitAdapter.checkout("master")

        gitAdapter.addCommitWithMessage("third")
        gitAdapter.checkout("branch1")
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit = gitAdapter.mergeInBranch("branch1", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit, preferredCommitIds = setOf(fourthCommit.id))

        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit),
            ),
            allPaths,
        )
    }

    @Test
    fun branchMergeBranchMergeFindsAllPaths() {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch1")
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit1 = gitAdapter.mergeInBranch("branch1", "merge")
        val fifthCommit = gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.switchToNewBranch("branch2")
        val sixthCommit = gitAdapter.addCommitWithMessage("sixth")
        gitAdapter.checkout("master")
        val seventhCommit = gitAdapter.addCommitWithMessage("seventh")
        val mergeCommit2 = gitAdapter.mergeInBranch("branch2", "merge")
        val eighthCommit = gitAdapter.addCommitWithMessage("eighth")

        gitAdapter.switchToNewBranch("branch3")
        val ninthCommit = gitAdapter.addCommitWithMessage("ninth")
        gitAdapter.checkout("master")
        val tenthCommit = gitAdapter.addCommitWithMessage("tenth")
        val mergeCommit3 = gitAdapter.mergeInBranch("branch3", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, mergeCommit3)

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
                ),
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
                ),
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
                ),
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
                ),
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
                ),
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
                ),
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
                ),
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
                ),
            ),
            allPaths,
        )
    }

    @Test
    fun willStopOnceFindingPathContainingAllPreferredCommits() {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch1")
        gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch1")
        gitAdapter.addCommitWithMessage("fourth")
        gitAdapter.checkout("master")
        val mergeCommit1 = gitAdapter.mergeInBranch("branch1", "merge")
        val fifthCommit = gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.switchToNewBranch("branch2")
        val sixthCommit = gitAdapter.addCommitWithMessage("sixth")
        gitAdapter.checkout("master")
        gitAdapter.addCommitWithMessage("seventh")
        val mergeCommit2 = gitAdapter.mergeInBranch("branch2", "merge")
        val eighthCommit = gitAdapter.addCommitWithMessage("eighth")

        gitAdapter.switchToNewBranch("branch3")
        gitAdapter.addCommitWithMessage("ninth")
        gitAdapter.checkout("master")
        val tenthCommit = gitAdapter.addCommitWithMessage("tenth")
        val mergeCommit3 = gitAdapter.mergeInBranch("branch3", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(
            log,
            mergeCommit3,
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
                ),
            ),
            allPaths,
        )
    }

    @Test
    fun mergeToBranchAndBackFindsAllPaths() {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        gitAdapter.checkout("branch")
        val mergeCommit1 = gitAdapter.mergeInBranch("master", "merge")
        val fourthCommit = gitAdapter.addCommitWithMessage("fourth")

        gitAdapter.checkout("master")
        val singleParentMerge = gitAdapter.mergeInBranch("branch", "merge")
        val fifthCommit = gitAdapter.addCommitWithMessage("fifth")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, fifthCommit)

        assertExpectedPaths(
            setOf(
                listOf(
                    fifthCommit,
                    singleParentMerge,
                    fourthCommit,
                    mergeCommit1,
                    thirdCommit,
                    firstCommit,
                ),
                listOf(
                    fifthCommit,
                    singleParentMerge,
                    fourthCommit,
                    mergeCommit1,
                    secondCommit,
                    firstCommit,
                ),
            ),
            allPaths,
        )
    }

    @Test
    fun branchOnBranchOnBranchFindsAllPaths() {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        gitAdapter.config("user.name", "Test")
        gitAdapter.config("user.email", "Test")

        val firstCommit = gitAdapter.show("HEAD")!!
        gitAdapter.switchToNewBranch("branch1")
        val secondCommit = gitAdapter.addCommitWithMessage("second")

        gitAdapter.checkout("master")
        val thirdCommit = gitAdapter.addCommitWithMessage("third")

        gitAdapter.switchToNewBranch("branch2")
        val fourthCommit = gitAdapter.addCommitWithMessage("forth")

        gitAdapter.checkout("master")
        val fifthCommit = gitAdapter.addCommitWithMessage("fifth")

        gitAdapter.checkout("branch1")
        val sixthCommit = gitAdapter.addCommitWithMessage("sixth")

        gitAdapter.checkout("master")
        val seventhCommit = gitAdapter.addCommitWithMessage("seventh")

        gitAdapter.checkout("branch2")
        val eighthCommit = gitAdapter.addCommitWithMessage("eighth")
        val merge1 = gitAdapter.mergeInBranch("branch1", "merge")
        val ninthCommit = gitAdapter.addCommitWithMessage("eighth")

        gitAdapter.checkout("master")
        val merge2 = gitAdapter.mergeInBranch("branch2", "merge")

        val log = diggerGitWrapper.log()

        val allPaths = allPaths(log, merge2)

        assertExpectedPaths(
            setOf(
                listOf(
                    merge2,
                    seventhCommit,
                    fifthCommit,
                    thirdCommit,
                    firstCommit,
                ),
                listOf(
                    merge2,
                    ninthCommit,
                    merge1,
                    eighthCommit,
                    fourthCommit,
                    thirdCommit,
                    firstCommit,
                ),
                listOf(
                    merge2,
                    ninthCommit,
                    merge1,
                    sixthCommit,
                    secondCommit,
                    firstCommit,
                ),
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
}
