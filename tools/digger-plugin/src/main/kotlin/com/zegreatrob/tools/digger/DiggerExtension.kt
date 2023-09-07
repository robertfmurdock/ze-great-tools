package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.allContributionCommits
import com.zegreatrob.tools.digger.core.contributionDataJson
import com.zegreatrob.tools.digger.core.currentContributionCommits
import org.ajoberstar.grgit.gradle.GrgitServiceExtension

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun allContributionData() = grgitServiceExtension.service.get().grgit
        .allContributionCommits()
        .map { range -> range.toList().contributionDataJson() }

    fun currentContributionData() = grgitServiceExtension.service.get().grgit
        .currentContributionCommits()
        .contributionDataJson()

    fun headId(): String = grgitServiceExtension.service.get().grgit.head().id
}
