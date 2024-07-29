package com.zegreatrob.tools.digger.core

import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.LogOp

fun Grgit.currentContributionCommits(): List<Commit> {
    val tag = previousTag()
    return if (tag == null) {
        log()
    } else {
        return log(
            fun(it: LogOp) {
                it.range(tag, "HEAD")
            },
        )
    }
}

private fun Grgit.previousTag(): Tag? {
    val tagList = orderedTagList()
    val tag = tagList.firstOrNull()
    return if (tag?.commit == head()) {
        tagList.getOrNull(1)
    } else {
        tag
    }
}

private fun Grgit.orderedTagList(): List<Tag> = tag.list().sortedByDescending { it.dateTime?.toInstant()?.toKotlinInstant() }

fun Grgit.currentCommitTag(): Tag? {
    val firstTag = orderedTagList()
        .also { it.forEach { println("tag ${it.name} ${it.dateTime?.toInstant()?.toKotlinInstant()}") } }
        .firstOrNull()
    return if (firstTag?.commit != head()) {
        null
    } else {
        firstTag
    }
}
