package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.wrapper.git.CommitRef
import com.zegreatrob.tools.wrapper.git.GitAdapter
import com.zegreatrob.tools.wrapper.git.TagRef

fun GitAdapter.findTrunkPath(tagRefs: List<TagRef>, log: List<CommitRef>): List<CommitRef> = allPaths(
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
