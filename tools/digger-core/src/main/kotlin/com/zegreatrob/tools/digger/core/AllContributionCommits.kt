package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import java.io.File

fun Grgit.allContributionCommits(workingDirectory: File): List<Pair<TagRef?, List<Commit>>> {
    val tagList = listTags(workingDirectory)
    return log().fold(emptyList()) { acc, commit ->
        val tag = tagList.find { it.commitId == commit.id }
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
