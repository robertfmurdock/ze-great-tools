package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag

fun Grgit.allContributionCommits(): List<Pair<Tag?, List<Commit>>> {
    val tagList = tag.list()
    return log().fold(emptyList()) { acc, commit ->
        val tag = tagList.find { it.commit == commit }
        if (tag != null) {
            acc.plus(element = tag to listOf(commit))
        } else {
            val lastPair = acc.lastOrNull()
            val lastList = lastPair?.second
            if (lastList != null) {
                acc.slice(0..acc.size - 2).plus(element = lastPair.copy(second = lastList.plusElement(commit)))
            } else {
                acc.plus(element = null to listOf(commit))
            }
        }
    }
}
