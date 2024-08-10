package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.currentContributionCommits(previousTag: TagRef?): List<CommitRef> = if (previousTag == null) {
    log()
} else {
    logWithRange(previousTag.name, "HEAD")
}
