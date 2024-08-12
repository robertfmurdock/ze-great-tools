package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.findTrunkPath(tagRefs: List<TagRef>, log: List<CommitRef>): List<CommitRef> = allPaths(
    log = log,
    firstTagCommit = log.first(),
    preferredCommitIds = tagRefs.map { it.commitId }.toSet(),
)
    .shortestPathWithMostTags(taggedCommitIds = tagRefs.map { it.commitId }.toSet())
    ?: log.alwaysLeftParent()

private fun MutableList<List<CommitRef>>.shortestPathWithMostTags(taggedCommitIds: Set<String>): List<CommitRef>? {
    val pathTagCountGroups = groupBy { path ->
        path.count { taggedCommitIds.contains(it.id) }
    }
    return pathTagCountGroups[pathTagCountGroups.keys.max()]
        ?.minBy { it.size }
}

fun List<CommitRef>.alwaysLeftParent() = fold(emptyList<CommitRef>()) { acc, commit ->
    if (acc.isEmpty()) {
        acc + commit
    } else if (commit.id == acc.last().parents.firstOrNull()) {
        acc + commit
    } else {
        acc
    }
}
