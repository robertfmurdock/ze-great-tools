package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import com.zegreatrob.tools.tagger.core.lastVersionAndTag
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import java.io.File

open class TaggerExtension(
    @Transient val rootProject: Project,
    objectFactory: ObjectFactory,
) {
    @Input
    var releaseBranch: String? = null

    @Input
    var userName: String? = null

    @Input
    var userEmail: String? = null

    @Input
    var warningsAsErrors = objectFactory.property<Boolean>().convention(false)

    @Input
    var workingDirectory = objectFactory.property<File>()

    @Input
    var implicitPatch = objectFactory.property<Boolean>().convention(true)

    @Input
    var disableDetached = objectFactory.property<Boolean>().convention(true)

    @Input
    var githubReleaseEnabled = objectFactory.property<Boolean>().convention(false)

    @Input
    var versionRegex = objectFactory.property<Regex?>().convention(null)

    @Input
    var noneRegex = objectFactory.property<Regex>().convention(VersionRegex.Defaults.none)

    @Input
    var patchRegex = objectFactory.property<Regex>().convention(VersionRegex.Defaults.patch)

    @Input
    var minorRegex = objectFactory.property<Regex>().convention(VersionRegex.Defaults.minor)

    @Input
    var majorRegex = objectFactory.property<Regex>().convention(VersionRegex.Defaults.major)

    val core get() = TaggerCore(GitAdapter(workingDirectory.get().absolutePath))

    fun lastVersionAndTag() = core.lastVersionAndTag()

    fun calculateVersion() = core.calculateNextVersion(
        implicitPatch = implicitPatch.get(),
        versionRegex = versionRegex(),
        disableDetached = disableDetached.get(),
        releaseBranch = releaseBranch ?: throw GradleException("Please configure the tagger release branch."),
    )

    private fun versionRegex() = VersionRegex(
        none = noneRegex.get(),
        patch = patchRegex.get(),
        minor = minorRegex.get(),
        major = majorRegex.get(),
        unified = versionRegex.orNull?.also { it.validateVersionRegex() },
    )
}

private fun Regex.validateVersionRegex() {
    if (
        VersionRegex.containsAllGroups(pattern)
    ) {
        return
    } else {
        throw GradleException(VersionRegex.MISSING_GROUP_ERROR)
    }
}
