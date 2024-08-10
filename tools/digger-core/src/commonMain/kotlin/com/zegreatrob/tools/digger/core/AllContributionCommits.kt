package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val log = log()
    val tags = listTags()
    val trunkPath = tags.findTrunkPath(log)
    return tags
        .relateToCommits(trunkPath)
        .foldInBranches(log - trunkPath.toSet())
        .map { (tag, commitSet) -> tag to commitSet.map { it.id }.toSet() }
        .withCommitsInOriginalOrder(log)
}

private fun List<TagRef>.findTrunkPath(log: List<CommitRef>) = allPaths(log, log.first(), map { it.commitId }.toSet())
    .shortestPathWithMostTags(taggedCommitIds = map { it.commitId }.toSet())
    ?: log.alwaysLeftParent()

private fun MutableList<List<CommitRef>>.shortestPathWithMostTags(taggedCommitIds: Set<String>): List<CommitRef>? {
    val pathTagCountGroups = groupBy { path ->
        path.count { taggedCommitIds.contains(it.id) }
    }
    return pathTagCountGroups[pathTagCountGroups.keys.max()]
        ?.minBy { it.size }
}

private fun List<Pair<TagRef?, Set<CommitRef>>>.foldInBranches(offMainCommits: List<CommitRef>) = reversed()
    .fold(emptyList<Pair<TagRef?, Set<CommitRef>>>()) { acc, (tag, commitSet) ->
        acc + (tag to foldInBranches(commitSet, offMainCommits - acc.flatMap { it.second }.toSet()))
    }.reversed()

private fun foldInBranches(
    commitSet: Set<CommitRef>,
    remainingOffMainCommits: List<CommitRef>,
): Set<CommitRef> = commitSet.fold(emptySet()) { acc, commit ->
    acc + commit + if (commit.parents.size <= 1) {
        emptyList()
    } else {
        branchCommits(
            commit.parents[0],
            remainingOffMainCommits - acc,
        ) + branchCommits(
            commit.parents[1],
            remainingOffMainCommits - acc,
        )
    }
}

private fun List<TagRef>.relateToCommits(trunkPath: List<CommitRef>): List<Pair<TagRef?, Set<CommitRef>>> {
    val tagSets: List<Pair<TagRef?, Set<CommitRef>>> =
        trunkPath.fold(emptyList()) { acc, commit ->
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

private fun List<CommitRef>.alwaysLeftParent() = fold(emptyList<CommitRef>()) { acc, commit ->
    if (acc.isEmpty()) {
        acc + commit
    } else if (commit.id == acc.last().parents.firstOrNull()) {
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
