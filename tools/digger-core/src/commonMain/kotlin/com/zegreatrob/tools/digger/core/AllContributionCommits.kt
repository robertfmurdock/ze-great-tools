package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val tagList = listTags()
    val log = log()
    val mainBranchLog = log.mainBranch()
    val mainBranchLogsWithFoldedBranches = mainBranchLog.foldInBranches(log)
    return tagList.relatedCommitIds(mainBranchLogsWithFoldedBranches)
        .withCommitsInOriginalOrder(log)
}

private fun List<TagRef>.relatedCommitIds(
    mainBranchLogsWithFoldedBranches: List<CommitRef>,
): List<Pair<TagRef?, Set<String>>> {
    val tagSets: List<Pair<TagRef?, Set<String>>> = mainBranchLogsWithFoldedBranches.fold(emptyList()) { acc, commit ->
        val tag = find { it.commitId == commit.id }
        if (tag != null) {
            acc.plus(element = tag to setOf(commit.id))
        } else {
            val lastPair = acc.lastOrNull()
            val lastList = lastPair?.second
            if (lastList != null) {
                acc.slice(0..acc.size - 2).plus(element = lastPair.copy(second = lastList.plusElement(commit.id)))
            } else {
                acc.plus(element = null to setOf(commit.id))
            }
        }
    }
    return tagSets
}

private fun List<CommitRef>.mainBranch() = fold(emptyList<CommitRef>()) { acc, commit ->
    if (acc.isEmpty()) {
        acc + commit
    } else if (commit.id == acc.last().parents.first()) {
        acc + commit
    } else {
        acc
    }
}

private fun List<CommitRef>.foldInBranches(log: List<CommitRef>): List<CommitRef> {
    val offMainCommits = log - toSet()
    return reversed()
        .fold<CommitRef, List<CommitRef>>(emptyList()) { acc, commit ->
            if (commit.parents.size > 1) {
                acc + branchCommits(
                    commit.parents[1],
                    offMainCommits - acc.toSet(),
                ) + listOf(commit)
            } else {
                acc + listOf(commit)
            }
        }
        .reversed()
}

private fun List<Pair<TagRef?, Set<String>>>.withCommitsInOriginalOrder(
    log: List<CommitRef>,
) = map { (tag, commitIds) ->
    tag to log.filter { commit -> commitIds.contains(commit.id) }
}

fun branchCommits(id: String?, offMainCommits: List<CommitRef>): List<CommitRef> = (0..offMainCommits.size)
    .fold(id to emptyList<CommitRef>()) { (id, foundCommits), _ ->
        val newCommit = offMainCommits.find { it.id == id }
        newCommit?.parents?.first() to (foundCommits + newCommit)
            .filterNotNull()
    }.second
