package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.currentContributionCommits(): List<CommitRef> {
    val tag = previousTag()
    return if (tag == null) {
        log()
    } else {
        return logWithRange(tag.name, "HEAD")
    }
}

private fun DiggerGitWrapper.previousTag(): TagRef? {
    val tagList = listTags().sortedByDescending { it.dateTime }
    val tag = tagList.firstOrNull()
    return if (tag?.commitId == headCommitId()) {
        tagList.getOrNull(1)
    } else {
        tag
    }
}
