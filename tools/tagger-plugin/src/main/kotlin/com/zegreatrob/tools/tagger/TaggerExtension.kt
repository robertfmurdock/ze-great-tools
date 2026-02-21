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
    var warningsAsErrors = objectFactory.property(Boolean::class.java).convention(false)

    @Input
    var workingDirectory = objectFactory.property(File::class.java)

    @Input
    var implicitPatch = objectFactory.property(Boolean::class.java).convention(true)

    @Input
    var disableDetached = objectFactory.property(Boolean::class.java).convention(true)

    @Input
    var forceSnapshot = objectFactory.property(Boolean::class.java).convention(false)

    @Input
    var githubReleaseEnabled = objectFactory.property(Boolean::class.java).convention(false)

    @Input
    var versionRegex = objectFactory.property(Regex::class.java).convention(null)

    @Input
    var noneRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.none)

    @Input
    var patchRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.patch)

    @Input
    var minorRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.minor)

    @Input
    var majorRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.major)

    val core get() = TaggerCore(GitAdapter(workingDirectory.get().absolutePath))

    fun lastVersionAndTag() = core.lastVersionAndTag()

    fun calculateVersion() = core.calculateNextVersion(
        implicitPatch = implicitPatch.get(),
        versionRegex = versionRegex(),
        disableDetached = disableDetached.get(),
        forceSnapshot = forceSnapshot.get(),
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
