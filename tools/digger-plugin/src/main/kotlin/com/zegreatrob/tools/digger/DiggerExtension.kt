package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import java.io.File

open class DiggerExtension(objectFactory: ObjectFactory) {
    @Input
    var label = objectFactory.property<String>()

    @Input
    var workingDirectory = objectFactory.property<File>()

    @Input
    var majorRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.majorRegex)

    @Input
    var minorRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.minorRegex)

    @Input
    var patchRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.patchRegex)

    @Input
    var noneRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.noneRegex)

    @Input
    var storyIdRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.storyIdRegex)

    @Input
    var easeRegex: Property<Regex> = objectFactory.property<Regex>().convention(MessageDigger.Defaults.easeRegex)

    @Input
    var tagRegex: Property<Regex> = objectFactory.property<Regex>().convention(DiggerCore.Defaults.tagRegex)

    private val gitWrapper get() = DiggerGitWrapper(workingDirectory.get().absolutePath)
    private val core
        get() = DiggerCore(
            label = label.get().ifBlank { null },
            tagRegex = tagRegex.get(),
            gitWrapper = gitWrapper,
            messageDigger = MessageDigger(
                majorRegex = majorRegex.get(),
                minorRegex = minorRegex.get(),
                patchRegex = patchRegex.get(),
                noneRegex = noneRegex.get(),
                storyIdRegex = storyIdRegex.get(),
                easeRegex = easeRegex.get(),
            ),
        )

    fun allContributionData() = core.allContributionData()

    fun currentContributionData() = core.currentContributionData()

    fun headId(): String = gitWrapper.headCommitId()
}
