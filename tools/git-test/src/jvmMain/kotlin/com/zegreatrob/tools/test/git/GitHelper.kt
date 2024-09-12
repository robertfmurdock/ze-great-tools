package com.zegreatrob.tools.test.git

import com.zegreatrob.tools.adapter.git.runProcess
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.MergeOp.Mode
import org.ajoberstar.grgit.operation.RemoteAddOp
import org.ajoberstar.grgit.operation.TagAddOp
import java.io.FileOutputStream

val defaultAuthors: List<String>
    get() = listOf("funk@test.io", "test@funk.edu")

fun initializeGitRepo(
    directory: String,
    remoteUrl: String = directory,
    addFileNames: Set<String>,
    commits: List<String> = listOf(),
    initialTag: String? = null,
): Grgit {
    val grgit = Grgit.init(mapOf("dir" to directory))
    disableGpgSign(directory)
    runProcess(listOf("git", "config", "user.useConfigOnly", "true"), directory)

    if (addFileNames.isNotEmpty()) {
        grgit.add(
            fun AddOp.() {
                patterns = addFileNames
            },
        )
    }
    commits.forEachIndexed { index, message ->
        grgit.addCommitWithMessage(message)
        if (index == 0 && initialTag != null) {
            grgit.addTag(initialTag)
        }
    }

    grgit.remote.add(
        fun RemoteAddOp.() {
            this.name = "origin"
            this.url = remoteUrl
        },
    )
    grgit.pull()
    grgit.branch.change(
        fun BranchChangeOp.() {
            this.name = "master"
            this.startPoint = "origin/master"
            this.mode = BranchChangeOp.Mode.TRACK
        },
    )
    return grgit
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

fun Grgit.ffOnlyInBranch(branchName: String) {
    merge {
        it.head = branchName
        it.setMode(Mode.ONLY_FF.name)
    }
}

fun disableGpgSign(projectDir: String) {
    FileOutputStream("$projectDir/.git/config", true)
        .writer().use {
            it.write("[commit]\n        gpgsign = false")
        }
}
