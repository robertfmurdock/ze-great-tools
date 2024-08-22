package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.TagRef

fun GitAdapter.allContributionCommits(
    tagRefs: List<TagRef>,
    fullLog: List<CommitRef>,
): List<Pair<TagRef?, List<CommitRef>>> = sortIntoTagSets(
    tagRefs = tagRefs
        .distinctBy { it.commitId }
        .filter { fullLog.map(CommitRef::id).contains(it.commitId) },
    log = fullLog,
)

fun GitAdapter.sortIntoTagSets(
    tagRefs: List<TagRef>,
    log: List<CommitRef>,
): List<Pair<TagRef?, List<CommitRef>>> = tagRefs.tagPairs()
    .map { (earlierTag, nextTag) ->
        earlierTag to if (earlierTag == null && nextTag != null) {
            logWithRange("^${nextTag.commitId}", "HEAD")
        } else if (earlierTag != null && nextTag != null) {
            logWithRange("^${nextTag.commitId}", earlierTag.commitId)
        } else if (earlierTag != null) {
            logWithRange(earlierTag.commitId)
        } else {
            log.alwaysLeftParent()
        }
    }.filter { it.second.isNotEmpty() }

private fun List<TagRef>.tagPairs(): List<Pair<TagRef?, TagRef?>> =
    listOf(Pair(null, firstOrNull())) + mapIndexed { index, commitRef ->
        commitRef to getOrNull(index + 1)
    }
