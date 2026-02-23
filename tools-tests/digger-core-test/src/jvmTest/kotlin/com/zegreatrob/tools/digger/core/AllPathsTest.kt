package com.zegreatrob.tools.digger.core

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.switchToNewBranch
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class AllPathsTest {

    @field:TempDir
    lateinit var projectDir: File

    private val diggerGitWrapper by lazy { GitAdapter(projectDir.absolutePath) }

    @Test
    fun willHandleSimplePathsEasily() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val secondCommit = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch1")
            addCommitWithMessage("second")
        }
        val thirdCommit = gitAdapter.run {
            checkout("master")
            addCommitWithMessage("third")
        }
        val fourthCommit = gitAdapter.run {
            checkout("branch1")
            addCommitWithMessage("fourth")
        }
        val mergeCommit = gitAdapter.run {
            checkout("master")
            mergeInBranch("branch1", "merge")
        }
    }) exercise {
        allPaths(diggerGitWrapper.log(), mergeCommit)
    } verify { allPaths ->
        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, thirdCommit, firstCommit),
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit),
            ),
            allPaths,
        )
    }

    @Test
    fun willIgnoreBranchesWhenOneParentIsInPreferredList() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val secondCommit = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch1")
            addCommitWithMessage("second")
        }
        val fourthCommit = gitAdapter.run {
            checkout("master")
            addCommitWithMessage("third")
            checkout("branch1")
            addCommitWithMessage("fourth")
        }
        val mergeCommit = gitAdapter.run {
            checkout("master")
            mergeInBranch("branch1", "merge")
        }
    }) exercise {
        allPaths(diggerGitWrapper.log(), mergeCommit, preferredCommitIds = setOf(fourthCommit.id))
    } verify { allPaths ->
        assertExpectedPaths(
            setOf(
                listOf(mergeCommit, fourthCommit, secondCommit, firstCommit),
            ),
            allPaths,
        )
    }

    @Test
    fun branchMergeBranchMergeFindsAllPaths() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val result1 = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch1")
            val second = addCommitWithMessage("second")
            checkout("master")
            val third = addCommitWithMessage("third")
            checkout("branch1")
            val fourth = addCommitWithMessage("fourth")
            checkout("master")
            val merge1 = mergeInBranch("branch1", "merge")
            val fifth = addCommitWithMessage("fifth")
            listOf(second, third, fourth, merge1, fifth)
        }
        val secondCommit = result1[0]
        val thirdCommit = result1[1]
        val fourthCommit = result1[2]
        val mergeCommit1 = result1[3]
        val fifthCommit = result1[4]

        val result2 = gitAdapter.run {
            switchToNewBranch("branch2")
            val sixth = addCommitWithMessage("sixth")
            checkout("master")
            val seventh = addCommitWithMessage("seventh")
            val merge2 = mergeInBranch("branch2", "merge")
            val eighth = addCommitWithMessage("eighth")
            listOf(sixth, seventh, merge2, eighth)
        }
        val sixthCommit = result2[0]
        val seventhCommit = result2[1]
        val mergeCommit2 = result2[2]
        val eighthCommit = result2[3]

        val result3 = gitAdapter.run {
            switchToNewBranch("branch3")
            val ninth = addCommitWithMessage("ninth")
            checkout("master")
            val tenth = addCommitWithMessage("tenth")
            val merge3 = mergeInBranch("branch3", "merge")
            listOf(ninth, tenth, merge3)
        }
        val ninthCommit = result3[0]
        val tenthCommit = result3[1]
        val mergeCommit3 = result3[2]
    }) exercise {
        allPaths(diggerGitWrapper.log(), mergeCommit3)
    } verify { allPaths ->
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
    fun willStopOnceFindingPathContainingAllPreferredCommits() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val result1 = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch1")
            addCommitWithMessage("second")
            checkout("master")
            val third = addCommitWithMessage("third")
            checkout("branch1")
            addCommitWithMessage("fourth")
            checkout("master")
            val merge1 = mergeInBranch("branch1", "merge")
            val fifth = addCommitWithMessage("fifth")
            listOf(third, merge1, fifth)
        }
        val thirdCommit = result1[0]
        val mergeCommit1 = result1[1]
        val fifthCommit = result1[2]

        val result2 = gitAdapter.run {
            switchToNewBranch("branch2")
            val sixth = addCommitWithMessage("sixth")
            checkout("master")
            addCommitWithMessage("seventh")
            val merge2 = mergeInBranch("branch2", "merge")
            val eighth = addCommitWithMessage("eighth")
            listOf(sixth, merge2, eighth)
        }
        val sixthCommit = result2[0]
        val mergeCommit2 = result2[1]
        val eighthCommit = result2[2]

        val result3 = gitAdapter.run {
            switchToNewBranch("branch3")
            addCommitWithMessage("ninth")
            checkout("master")
            val tenth = addCommitWithMessage("tenth")
            val merge3 = mergeInBranch("branch3", "merge")
            listOf(tenth, merge3)
        }
        val tenthCommit = result3[0]
        val mergeCommit3 = result3[1]
    }) exercise {
        allPaths(
            diggerGitWrapper.log(),
            mergeCommit3,
            preferredCommitIds = setOf(tenthCommit.id, sixthCommit.id, thirdCommit.id),
        )
    } verify { allPaths ->
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
    fun mergeToBranchAndBackFindsAllPaths() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val result1 = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch")
            val second = addCommitWithMessage("second")
            checkout("master")
            val third = addCommitWithMessage("third")
            checkout("branch")
            val merge1 = mergeInBranch("master", "merge")
            val fourth = addCommitWithMessage("fourth")
            checkout("master")
            val spMerge = mergeInBranch("branch", "merge")
            val fifth = addCommitWithMessage("fifth")
            listOf(second, third, merge1, fourth, spMerge, fifth)
        }
        val secondCommit = result1[0]
        val thirdCommit = result1[1]
        val mergeCommit1 = result1[2]
        val fourthCommit = result1[3]
        val singleParentMerge = result1[4]
        val fifthCommit = result1[5]
    }) exercise {
        allPaths(diggerGitWrapper.log(), fifthCommit)
    } verify { allPaths ->
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
    fun branchOnBranchOnBranchFindsAllPaths() = setup(object {
        val gitAdapter = initializeGitRepo(
            directory = projectDir.absolutePath,
            commits = listOf("first"),
            addFileNames = emptySet(),
        )
        val firstCommit = gitAdapter.show("HEAD")!!
        val result1 = gitAdapter.run {
            config("user.name", "Test")
            config("user.email", "Test")
            switchToNewBranch("branch1")
            val second = addCommitWithMessage("second")
            checkout("master")
            val third = addCommitWithMessage("third")
            switchToNewBranch("branch2")
            val fourth = addCommitWithMessage("forth")
            checkout("master")
            val fifth = addCommitWithMessage("fifth")
            checkout("branch1")
            val sixth = addCommitWithMessage("sixth")
            checkout("master")
            val seventh = addCommitWithMessage("seventh")
            checkout("branch2")
            val eighth = addCommitWithMessage("eighth")
            val m1 = mergeInBranch("branch1", "merge")
            val ninth = addCommitWithMessage("eighth")
            checkout("master")
            val m2 = mergeInBranch("branch2", "merge")
            listOf(second, third, fourth, fifth, sixth, seventh, eighth, m1, ninth, m2)
        }
        val secondCommit = result1[0]
        val thirdCommit = result1[1]
        val fourthCommit = result1[2]
        val fifthCommit = result1[3]
        val sixthCommit = result1[4]
        val seventhCommit = result1[5]
        val eighthCommit = result1[6]
        val merge1 = result1[7]
        val ninthCommit = result1[8]
        val merge2 = result1[9]
    }) exercise {
        allPaths(diggerGitWrapper.log(), merge2)
    } verify { allPaths ->
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
        allPaths.toSet().justIds().assertIsEqualTo(
            expected.justIds(),
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
