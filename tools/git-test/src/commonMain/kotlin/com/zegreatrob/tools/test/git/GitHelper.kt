package com.zegreatrob.tools.test.git

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.TagRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

val defaultAuthors: List<String>
    get() = listOf("funk@test.io", "test@funk.edu")

fun initializeGitRepo(
    directory: String,
    remoteUrl: String = directory,
    addFileNames: Set<String>,
    commits: List<String> = listOf(),
    initialTag: String? = null,
): GitAdapter {
    val gitAdapter = GitAdapter(
        directory,
        mapOf(
            "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
            "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
        ),
    )
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
    if (remoteUrl != directory) {
        gitAdapter.push(true, upstream = "origin", branch = "master")
    } else {
        gitAdapter.setBranchUpstream("origin/master", "master")
    }
    return gitAdapter
}

fun GitAdapter.addTag(initialTag: String): TagRef {
    newAnnotatedTag(initialTag, "HEAD", null, null)
    return showTag("HEAD")!!
}

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

suspend fun delayLongEnoughToAffectGitDate() {
    withContext(Dispatchers.Default) {
        delay(1.seconds)
    }
}

fun GitAdapter.switchToNewBranch(name: String) {
    checkout(branch = name, newBranch = true)
}

fun GitAdapter.mergeInBranch(branchName: String, message: String): CommitRef {
    merge(branch = branchName, noCommit = true, ffOnly = false)
    return addCommitWithMessage(message)
}

fun GitAdapter.ffOnlyInBranch(branchName: String) {
    merge(branch = branchName, noCommit = false, ffOnly = true)
}
