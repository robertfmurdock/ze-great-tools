package com.zegreatrob.tools.tagger

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property

open class TaggerExtension(
    val grgitServiceExtension: GrgitServiceExtension,
    @Transient val rootProject: Project,
    objectFactory: ObjectFactory,
) {

    @Input
    var releaseBranch: String? = null

    @Input
    var implicitPatch = objectFactory.property<Boolean>().convention(true)

    @Input
    var githubReleaseEnabled = objectFactory.property<Boolean>().convention(false)

    @Input
    var noneRegex = objectFactory.property<Regex>().convention(Regex("\\[none].*"))

    @Input
    var patchRegex = objectFactory.property<Regex>().convention(Regex("\\[patch].*"))

    @Input
    var minorRegex = objectFactory.property<Regex>().convention(Regex("\\[minor].*"))

    @Input
    var majorRegex = objectFactory.property<Regex>().convention(Regex("\\[major].*"))

    val version by lazy {
        calculateBuildVersion(
            grgitServiceExtension.service.get().grgit,
            releaseBranch
                ?: throw GradleException("Please configure the tagger release branch."),
        )
    }

    val isSnapshot get() = version.contains("SNAPSHOT")

    private fun calculateBuildVersion(grgit: Grgit, releaseBranch: String) = grgit.calculateNextVersion(implicitPatch.get(), versionRegex(), releaseBranch)

    private fun versionRegex() = VersionRegex(
        none = noneRegex.get(),
        patch = patchRegex.get(),
        minor = minorRegex.get(),
        major = majorRegex.get(),
    )
}
