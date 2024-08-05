package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val tagList = listTags()
    val log = log()

    val mainBranchLog = log.fold(emptyList<CommitRef>()) { acc, commit ->
        if (acc.isEmpty()) {
            acc + commit
        } else if (commit.id == acc.last().parents.first()) {
            acc + commit
        } else {
            acc
        }
    }

    val offMainCommits = log - mainBranchLog.toSet()

    val mainBranchLogsWithFoldedBranches = mainBranchLog.flatMap { commit ->

        if (commit.parents.size > 1) {
            listOf(commit) + branchCommits(commit.parents[1], offMainCommits)
        } else {
            listOf(commit)
        }
    }

    return mainBranchLogsWithFoldedBranches.fold(emptyList()) { acc, commit ->
        val tag = tagList.find { it.commitId == commit.id }
        if (tag != null) {
            acc.plus(element = tag to listOf(commit))
        } else {
            val lastPair = acc.lastOrNull()
            val lastList = lastPair?.second
            if (lastList != null) {
                acc.slice(0..acc.size - 2).plus(element = lastPair.copy(second = lastList.plusElement(commit)))
            } else {
                acc.plus(element = null to listOf(commit))
            }
        }
    }
}

fun branchCommits(id: String?, offMainCommits: List<CommitRef>): List<CommitRef> = (0..offMainCommits.size)
    .fold(id to emptyList<CommitRef>()) { (id, foundCommits), _ ->
        val newCommit = offMainCommits.find { it.id == id }
        newCommit?.parents?.first() to (foundCommits + newCommit)
            .filterNotNull()
    }.second
