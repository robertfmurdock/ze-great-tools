package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import com.zegreatrob.tools.tagger.core.lastVersionAndTag
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory

open class TaggerExtension(
    objectFactory: ObjectFactory,
) {
    internal val releaseBranchProperty = objectFactory.property(String::class.java)
    var releaseBranch: String?
        get() = releaseBranchProperty.orNull
        set(value) {
            value?.let { releaseBranchProperty.set(it) }
        }

    internal val userNameProperty = objectFactory.property(String::class.java)
    var userName: String?
        get() = userNameProperty.orNull
        set(value) {
            value?.let { userNameProperty.set(it) }
        }

    internal val userEmailProperty = objectFactory.property(String::class.java)
    var userEmail: String?
        get() = userEmailProperty.orNull
        set(value) {
            value?.let { userEmailProperty.set(it) }
        }

    val warningsAsErrors = objectFactory.property(Boolean::class.java).convention(false)

    val workingDirectory = objectFactory.directoryProperty()

    val implicitPatch = objectFactory.property(Boolean::class.java).convention(true)

    val disableDetached = objectFactory.property(Boolean::class.java).convention(true)

    val forceSnapshot = objectFactory.property(Boolean::class.java).convention(false)

    val githubReleaseEnabled = objectFactory.property(Boolean::class.java).convention(false)

    val versionRegex = objectFactory.property(Regex::class.java).convention(null)

    val noneRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.none)

    val patchRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.patch)

    val minorRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.minor)

    val majorRegex = objectFactory.property(Regex::class.java).convention(VersionRegex.Defaults.major)

    val core get() = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))

    fun lastVersionAndTag() = core.lastVersionAndTag()

    fun calculateVersion() = core.calculateNextVersion(
        implicitPatch = implicitPatch.get(),
        versionRegex = versionRegex(),
        disableDetached = disableDetached.get(),
        forceSnapshot = forceSnapshot.get(),
        releaseBranch = releaseBranchProperty.orNull
            ?: throw GradleException("Please configure the tagger release branch."),
    )

    private fun versionRegex() = VersionRegex(
        none = noneRegex.get(),
        patch = patchRegex.get(),
        minor = minorRegex.get(),
        major = majorRegex.get(),
        unified = versionRegex.orNull?.also { it.validateVersionRegex() },
    )
}

internal fun Regex.validateVersionRegex() {
    if (
        VersionRegex.containsAllGroups(pattern)
    ) {
        return
    } else {
        throw GradleException(VersionRegex.MISSING_GROUP_ERROR)
    }
}
