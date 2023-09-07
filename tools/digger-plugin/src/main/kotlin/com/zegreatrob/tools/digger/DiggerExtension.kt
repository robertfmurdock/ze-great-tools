package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.allContributionCommits
import com.zegreatrob.tools.digger.core.contributionDataJson
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.operation.LogOp

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun allContributionData() = grgitServiceExtension.service.get().grgit
        .allContributionCommits()
        .map { range -> range.toList().contributionDataJson() }

    fun currentContributionData() = grgitServiceExtension.service.get().grgit
        .currentContributionCommits()
        .contributionDataJson()

    private fun Grgit.currentContributionCommits(): List<Commit> {
        val tag = previousTag()
        return if (tag == null) {
            log()
        } else {
            return log(fun(it: LogOp) { it.range(tag, "HEAD") })
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

    fun headId(): String = grgitServiceExtension.service.get().grgit.head().id
}
