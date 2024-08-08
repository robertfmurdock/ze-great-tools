package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val log = log()
    val tags = listTags()
    val trunkPath = tags.reversed().findTrunkPath(log)
    return tags
        .relateToCommits(trunkPath)
        .foldInBranches(log - trunkPath.toSet())
        .map { (tag, commitSet) -> tag to commitSet.map { it.id }.toSet() }
        .withCommitsInOriginalOrder(log)
}

private fun List<TagRef>.findTrunkPath(log: List<CommitRef>): List<CommitRef> {
    val latestTag = firstOrNull()
    val firstTagCommit = log.reversed().find { it.id == latestTag?.commitId } ?: return log.alwaysLeftParent()
    val allPaths = allPaths(log, firstTagCommit)

    val remainingTags = this - latestTag

    val pathTagComparison = allPaths.groupBy { path ->
        remainingTags
            .count { tag ->
                path.map { it.id }.contains(tag?.commitId)
            }
    }
    val alwaysLeftLog = log.alwaysLeftParent()
    val pathFromLatestTag = pathTagComparison[pathTagComparison.keys.max()]?.firstOrNull()

    val path = if (pathFromLatestTag != null) {
        alwaysLeftLog.subList(0, alwaysLeftLog.indexOf(firstTagCommit)) + pathFromLatestTag
    } else {
        null
    }

    return path ?: alwaysLeftLog
}

private fun allPaths(
    log: List<CommitRef>,
    firstTagCommit: CommitRef
): MutableList<List<CommitRef>> {
    val commitMap = log.associateBy { it.id }
    val allPaths = mutableListOf<List<CommitRef>>()

    var currentPath = emptyList<CommitRef>()
    var pendingCommits = listOf<Pair<CommitRef, CommitRef?>>(firstTagCommit to null)
    while (pendingCommits.isNotEmpty()) {
        val currentEntry = pendingCommits.last()
        val (currentCommit, child) = currentEntry
        pendingCommits = pendingCommits - currentEntry

        currentPath = currentPath.takeWhile { it != child } + listOfNotNull(child, currentCommit)
        print("currentPath: $currentPath")
        if (currentCommit.parents.isEmpty()) {
            allPaths += currentPath
            println("path: ${currentPath.joinToString(", ")}")
        } else {
            val parentRefs = currentCommit.parents.mapNotNull { commitMap[it] }
            pendingCommits = pendingCommits + parentRefs.map { it to currentCommit }
        }
    }
    return allPaths
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

private fun List<TagRef>.relateToCommits(
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

private fun List<CommitRef>.alwaysLeftParent() = fold(emptyList<CommitRef>()) { acc, commit ->
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
