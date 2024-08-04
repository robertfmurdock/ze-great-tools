package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.core.TagRef
import com.zegreatrob.tools.digger.core.allContributionCommits
import com.zegreatrob.tools.digger.core.contribution
import com.zegreatrob.tools.digger.model.Contribution
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import java.io.File

open class DiggerExtension(objectFactory: ObjectFactory) {
    @Input
    var label = objectFactory.property<String>()

    @Input
    var workingDirectory = objectFactory.property<File>()

    @Input
    var majorRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.majorRegex)

    @Input
    var minorRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.minorRegex)

    @Input
    var patchRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.patchRegex)

    @Input
    var noneRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.noneRegex)

    @Input
    var storyIdRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.storyIdRegex)

    @Input
    var easeRegex = objectFactory.property<Regex>().convention(MessageDigger.Defaults.easeRegex)

    private val gitWrapper get() = DiggerGitWrapper(workingDirectory.get().absolutePath)
    private val messageDigger
        get() = MessageDigger(
            majorRegex = majorRegex.get(),
            minorRegex = minorRegex.get(),
            patchRegex = patchRegex.get(),
            noneRegex = noneRegex.get(),
            storyIdRegex = storyIdRegex.get(),
            easeRegex = easeRegex.get(),
        )
    private val core get() = DiggerCore(label.get().ifBlank { null }, gitWrapper, messageDigger)

    fun allContributionData() = gitWrapper
        .allContributionCommits()
        .map { range -> range.first to messageDigger.contribution(range.second.toList()) }
        .map { (tag, contributions) -> contributions.copyWithLabelAndTag(tag) }

    fun currentContributionData() = core.currentContributionData()

    private fun Contribution.copyWithLabelAndTag(currentCommitTag: TagRef?) = copy(
        label = this@DiggerExtension.label.get(),
        tagName = currentCommitTag?.name,
        tagDateTime = currentCommitTag?.dateTime,
    )

    fun headId(): String = gitWrapper.headCommitId()
}
