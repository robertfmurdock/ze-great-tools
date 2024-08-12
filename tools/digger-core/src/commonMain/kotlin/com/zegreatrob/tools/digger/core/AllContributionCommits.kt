package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.allContributionCommits(): List<Pair<TagRef?, List<CommitRef>>> {
    val log = log()
    val tags = listTags()
        .distinctBy { it.commitId }
        .filter { log.map(CommitRef::id).contains(it.commitId) }

    return sortIntoTagSets(tags, log)
}

fun DiggerGitWrapper.sortIntoTagSets(
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
