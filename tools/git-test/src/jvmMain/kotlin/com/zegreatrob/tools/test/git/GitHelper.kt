package com.zegreatrob.tools.test.git

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.MergeOp.Mode
import org.ajoberstar.grgit.operation.TagAddOp

val defaultAuthors: List<String>
    get() = listOf("funk@test.io", "test@funk.edu")

fun initializeGitRepo(
    directory: String,
    remoteUrl: String = directory,
    addFileNames: Set<String>,
    commits: List<String> = listOf(),
    initialTag: String? = null,
): Pair<Grgit, GitAdapter> {
    val gitAdapter = GitAdapter(directory)
    gitAdapter.init()
    gitAdapter.config("commit.gpgsign", "false")
    gitAdapter.config("user.useConfigOnly", "true")

    if (addFileNames.isNotEmpty()) {
        gitAdapter.add(files = addFileNames.toTypedArray())
    }
    commits.forEachIndexed { index, message ->
        gitAdapter.addCommitWithMessage(message)
        if (index == 0 && initialTag != null) {
            gitAdapter.newAnnotatedTag(initialTag, "HEAD", "Funky Testerson", "funk@test.io")
        }
    }
    gitAdapter.addRemote(name = "origin", url = remoteUrl)
    gitAdapter.fetch()
    val grgit = Grgit.open(mapOf("dir" to directory))
    grgit.branch.change(
        fun BranchChangeOp.() {
            this.name = "master"
            this.startPoint = "origin/master"
            this.mode = BranchChangeOp.Mode.TRACK
        },
    )
    return grgit to gitAdapter
}

fun Grgit.addTag(initialTag: String?): org.ajoberstar.grgit.Tag? = tag.add(
    fun(it: TagAddOp) {
        it.name = initialTag
    },
)

fun Grgit.addCommitWithMessage(message: String): Commit =
    commit(
        fun(it: CommitOp) {
            it.author = org.ajoberstar.grgit.Person("Funky Testerson", "funk@test.io")
            it.committer = org.ajoberstar.grgit.Person("Testy Funkerson", "test@funk.edu")
            it.message = message
        },
    )

fun GitAdapter.addCommitWithMessage(message: String): CommitRef {
    commit(
        message = message,
        authorName = "Funky Testerson",
        authorEmail = "funk@test.io",
        committerName = "Testy Funkerson",
        committerEmail = "test@funk.edu",
    )
    return show("HEAD")!!
}

fun delayLongEnoughToAffectGitDate() {
    Thread.sleep(1000)
}

fun Grgit.switchToNewBranch(name: String) {
    branch.add { it.name = name }
    checkout { it.branch = name }
}

fun Grgit.mergeInBranch(branchName: String, message: String): Commit {
    merge {
        it.head = branchName
        it.setMode("no-commit")
    }
    return addCommitWithMessage(message)
}

fun GitAdapter.mergeInBranch(branchName: String, message: String): CommitRef {
    merge(branch = branchName, noCommit = true)
    return addCommitWithMessage(message)
}

fun Grgit.ffOnlyInBranch(branchName: String) {
    merge {
        it.head = branchName
        it.setMode(Mode.ONLY_FF.name)
    }
}
