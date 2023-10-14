package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.LogOp

fun Grgit.currentContributionCommits(): List<Commit> {
    val tag = previousTag()
    return if (tag == null) {
        log()
    } else {
        return log(fun(it: LogOp) {
            it.range(tag, "HEAD")
        })
    }
}

private fun Grgit.previousTag(): Tag? {
    val tagList = tag.list().sortedByDescending { it.dateTime }
    val tag = tagList.firstOrNull()
    return if (tag?.commit == head()) {
        tagList.getOrNull(1)
    } else {
        tag
    }
}
