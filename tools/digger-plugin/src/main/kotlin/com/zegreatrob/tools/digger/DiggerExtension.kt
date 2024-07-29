package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.allContributionCommits
import com.zegreatrob.tools.digger.core.contribution
import com.zegreatrob.tools.digger.core.currentCommitTag
import com.zegreatrob.tools.digger.core.currentContributionCommits
import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
    objectFactory: ObjectFactory,
) {
    @Input
    var label = objectFactory.property<String>()

    fun allContributionData() =
        grgitServiceExtension.service.get().grgit
            .allContributionCommits()
            .map { range -> range.first to range.second.toList().contribution() }
            .map { (tag, contributions) -> contributions.copyWithLabelAndTag(tag) }

    fun currentContributionData() =
        with(grgitServiceExtension.service.get().grgit) {
            val currentCommitTag = currentCommitTag()
            currentContributionCommits()
                .contribution()
                .copyWithLabelAndTag(currentCommitTag)
        }

    private fun Contribution.copyWithLabelAndTag(currentCommitTag: Tag?) = copy(
        label = this@DiggerExtension.label.get(),
        tagName = currentCommitTag?.name,
        tagDateTime = currentCommitTag?.dateTime?.toInstant()?.toKotlinInstant(),
    )

    fun headId(): String = grgitServiceExtension.service.get().grgit.head().id
}
