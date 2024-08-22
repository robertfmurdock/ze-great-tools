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
    var workingDirectory = objectFactory.property<File>()

    @Input
    var implicitPatch = objectFactory.property<Boolean>().convention(true)

    @Input
    var githubReleaseEnabled = objectFactory.property<Boolean>().convention(false)

    @Input
    var versionRegex = objectFactory.property<Regex?>().convention(null)

    @Input
    var noneRegex = objectFactory.property<Regex>().convention(Regex("\\[none].*", RegexOption.IGNORE_CASE))

    @Input
    var patchRegex = objectFactory.property<Regex>().convention(Regex("\\[patch].*", RegexOption.IGNORE_CASE))

    @Input
    var minorRegex = objectFactory.property<Regex>().convention(Regex("\\[minor].*", RegexOption.IGNORE_CASE))

    @Input
    var majorRegex = objectFactory.property<Regex>().convention(Regex("\\[major].*", RegexOption.IGNORE_CASE))

    val gitAdapter get() = GitAdapter(workingDirectory.get().absolutePath)
    val core get() = TaggerCore(GitAdapter(workingDirectory.get().absolutePath))

    val lastVersionAndTag by lazy { core.lastVersionAndTag() }

    val version by lazy {
        val (previousVersionNumber, lastTagDescription) =
            lastVersionAndTag
                ?: return@lazy "0.0.0"
        core.calculateNextVersion(
            lastTagDescription = lastTagDescription,
            implicitPatch = implicitPatch.get(),
            versionRegex = versionRegex(),
            previousVersionNumber = previousVersionNumber,
            releaseBranch = releaseBranch ?: throw GradleException("Please configure the tagger release branch."),
        )
    }

    private fun versionRegex() =
        VersionRegex(
            none = noneRegex.get(),
            patch = patchRegex.get(),
            minor = minorRegex.get(),
            major = majorRegex.get(),
            unified = versionRegex.orNull?.also { it.validateVersionRegex() },
        )
}

private fun Regex.validateVersionRegex() {
    if (
        pattern.contains("?<major>") &&
        pattern.contains("?<minor>") &&
        pattern.contains("?<patch>") &&
        pattern.contains("?<none>")
    ) {
        return
    } else {
        throw GradleException("version regex must include groups named 'major', 'minor', 'patch', and 'none'.")
    }
}
