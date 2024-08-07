package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val tagList = listTags()
    val log = log()
    val mainBranchLog = log.mainBranch()
    val contributionCommits = tagList.relatedCommits(mainBranchLog)
    return contributionCommits.foldInBranches(log - mainBranchLog.toSet())
        .map { (tag, commitSet) -> tag to commitSet.map { it.id }.toSet() }
        .withCommitsInOriginalOrder(log)
}

private fun List<Pair<TagRef?, Set<CommitRef>>>.foldInBranches(
    offMainCommits: List<CommitRef>,
): List<Pair<TagRef?, List<CommitRef>>> = reversed()
    .fold<Pair<TagRef?, Set<CommitRef>>, List<Pair<TagRef?, List<CommitRef>>>>(emptyList()) { acc, (tag, commitSet) ->
        acc + (
            tag to run {
                commitSet.toList().reversed()
                    .fold<CommitRef, List<CommitRef>>(emptyList()) { acc2, commit ->

                        if (commit.parents.size > 1) {
                            acc2 + (
                                branchCommits(
                                    commit.parents[1],
                                    offMainCommits - acc2.toSet() - acc.flatMap { it.second }.toSet(),
                                ) + listOf(commit)
                                )
                        } else {
                            acc2 + listOf(commit)
                        }
                    }
                    .reversed()
            }
            )
    }.reversed()

private fun List<TagRef>.relatedCommits(
    mainBranchLogsWithFoldedBranches: List<CommitRef>,
): List<Pair<TagRef?, Set<CommitRef>>> {
    val tagSets: List<Pair<TagRef?, Set<CommitRef>>> =
        mainBranchLogsWithFoldedBranches.fold(emptyList()) { acc, commit ->
            val tag = find { it.commitId == commit.id }
            if (tag != null) {
                acc.plus(element = tag to setOf(commit))
            } else {
                val lastPair = acc.lastOrNull()
                val lastList = lastPair?.second
                if (lastList != null) {
                    acc.slice(0..acc.size - 2).plus(element = lastPair.copy(second = lastList.plusElement(commit)))
                } else {
                    acc.plus(element = null to setOf(commit))
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

private fun List<Pair<TagRef?, Set<String>>>.withCommitsInOriginalOrder(
    log: List<CommitRef>,
) = map { (tag, commitIds) ->
    tag to log.filter { commit -> commitIds.contains(commit.id) }
}

fun branchCommits(id: String, offMainCommits: List<CommitRef>): List<CommitRef> = (0..offMainCommits.size)
    .fold(listOf(id) to emptyList<CommitRef>()) { (ids, foundCommits), _ ->
        val newCommits = offMainCommits.filter { ids.contains(it.id) }
        newCommits.flatMap { it.parents } to (foundCommits + newCommits)
    }.second
