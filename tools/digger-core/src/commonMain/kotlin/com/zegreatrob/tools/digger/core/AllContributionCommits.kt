package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val log = log()
    val mainBranchLog = log.alwaysLeftParent()
    val tags = listTags()
    println("tags\n${tags.joinToString("\n")}")

    val trunkPath = tags.reversed().findTrunkPath(log)
    println("mainBranchLog\n${mainBranchLog.joinToString("\n") { it.id }}")
    println("trunkPath\n${trunkPath.joinToString("\n") { it.id }}")

    return tags
        .relateToCommits(trunkPath)
        .foldInBranches(log - trunkPath.toSet())
        .map { (tag, commitSet) -> tag to commitSet.map { it.id }.toSet() }
        .withCommitsInOriginalOrder(log)
}

private fun List<TagRef>.findTrunkPath(log: List<CommitRef>): List<CommitRef> {
    val latestTag = firstOrNull()
    val firstTagCommit = log.reversed().find { it.id == latestTag?.commitId }
    println("before all paths")
    val treeCache = mutableMapOf<String, TreeRef>()
    val allPaths = firstTagCommit?.parentTree(log.associateBy { it.id }, treeCache)?.flatten()
    println("after all paths")

    val remainingTags = this - latestTag

    val pathTagComparison =
        allPaths?.groupBy { path -> remainingTags.count { tag -> path.map { it.id }.contains(tag?.commitId) } }
    val alwaysLeftLog = log.alwaysLeftParent()
    val pathFromLatestTag = pathTagComparison?.get(pathTagComparison.keys.max())?.firstOrNull()

    val path = if (pathFromLatestTag != null) {
        alwaysLeftLog.subList(0, alwaysLeftLog.indexOf(firstTagCommit)) + pathFromLatestTag
    } else {
        null
    }

    return path ?: alwaysLeftLog
}

private fun CommitRef.parentTree(log: Map<String, CommitRef>, treeCache: MutableMap<String, TreeRef>): TreeRef = TreeRef(
    this,
    parentRefs(this, log)
        .map { treeCache.getOrPut(it.id) { it.parentTree(log, treeCache) } },
)

data class TreeRef(val commitRef: CommitRef, val parentTree: List<TreeRef>) {
    fun flatten(): List<List<CommitRef>> = if (parentTree.isEmpty()) {
        listOf(listOf(commitRef))
    } else {
        parentTree.map { it.flatten() }.flatten().map { listOf(commitRef) + it }
    }.also { println("flattened ${commitRef.id}") }
}

private fun parentRefs(
    commit: CommitRef,
    log: Map<String, CommitRef>,
) = commit.parents.mapNotNull { parentId -> log[parentId] }
    .also { println("parent refs for ${commit.id}") }

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
