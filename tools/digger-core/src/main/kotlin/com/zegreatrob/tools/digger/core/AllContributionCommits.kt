package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit

fun Grgit.allContributionCommits(): List<List<Commit>> {
    val tagList = tag.list()
    return log().fold(emptyList()) { acc, commit ->
        if (tagList.any { it.commit == commit }) {
            acc.plus(element = listOf(commit))
        } else {
            val lastList = acc.lastOrNull()
            if (lastList != null) {
                acc.slice(0..acc.size - 2).plus(element = lastList.plusElement(commit))
            } else {
                acc.plus(element = listOf(commit))
            }
        }
    }
}
