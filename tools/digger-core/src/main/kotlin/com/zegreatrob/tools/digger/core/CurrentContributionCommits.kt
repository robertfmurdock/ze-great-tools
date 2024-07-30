package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.LogOp

fun Grgit.currentContributionCommits(wrapper: DiggerGitWrapper): List<Commit> {
    val tag = wrapper.previousTag()
    return if (tag == null) {
        log()
    } else {
        return log(
            fun(it: LogOp) {
                it.range(tag.name, "HEAD")
            },
        )
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
