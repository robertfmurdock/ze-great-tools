package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import com.zegreatrob.tools.tagger.core.lastVersionAndTag
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import java.io.File

open class TaggerExtension(
    objectFactory: ObjectFactory,
) {
    val workingDirectory = objectFactory.directoryProperty()

    private val fileConfig = workingDirectory.map { dir -> readTaggerFileFrom(dir.asFile) ?: TaggerConfig() }

    internal val releaseBranchProperty = objectFactory.property(String::class.java).apply {
        convention(fileConfig.map { it.releaseBranch })
    }
    var releaseBranch: String?
        get() = releaseBranchProperty.orNull
        set(value) {
            value?.let { releaseBranchProperty.set(it) }
        }

    internal val userNameProperty = objectFactory.property(String::class.java).apply {
        convention(fileConfig.map { it.userName })
    }
    var userName: String?
        get() = userNameProperty.orNull
        set(value) {
            value?.let { userNameProperty.set(it) }
        }

    internal val userEmailProperty = objectFactory.property(String::class.java).apply {
        convention(fileConfig.map { it.userEmail })
    }
    var userEmail: String?
        get() = userEmailProperty.orNull
        set(value) {
            value?.let { userEmailProperty.set(it) }
        }

    val warningsAsErrors = objectFactory.property(Boolean::class.java).apply {
        convention(fileConfig.map { it.warningsAsErrors ?: false })
    }

    val implicitPatch = objectFactory.property(Boolean::class.java).apply {
        convention(fileConfig.map { it.implicitPatch ?: true })
    }

    /**
     * When true (default), versioning fails if HEAD has no upstream tracking branch (detached HEAD).
     * Fix the CI checkout step instead of disabling this check.
     * See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md
     * @deprecated Use allowDetachedHead instead (inverted logic).
     */
    @Deprecated("Use allowDetachedHead instead (inverted logic)")
    val disableDetached = objectFactory.property(Boolean::class.java).apply {
        convention(fileConfig.map { it.disableDetached ?: true })
    }

    /**
     * When true, versioning allows detached HEAD (no upstream tracking branch).
     * When false (default), versioning fails on detached HEAD.
     * Fix the CI checkout step instead of allowing detached HEAD.
     * See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md
     */
    internal val allowDetachedHeadProperty = objectFactory.property(Boolean::class.java).apply {
        convention(fileConfig.map { it.allowDetachedHead })
    }
    var allowDetachedHead: Boolean?
        get() = allowDetachedHeadProperty.orNull
        set(value) {
            value?.let { allowDetachedHeadProperty.set(it) }
        }

    val forceSnapshot = objectFactory.property(Boolean::class.java).apply {
        convention(fileConfig.map { it.forceSnapshot ?: false })
    }

    val githubReleaseEnabled = objectFactory.property(Boolean::class.java).convention(false)

    val versionRegex = objectFactory.property(Regex::class.java).apply {
        convention(fileConfig.map { it.versionRegex?.let { pattern -> Regex(pattern) } })
    }

    val noneRegex = objectFactory.property(Regex::class.java).apply {
        convention(fileConfig.map { it.noneRegex?.let { pattern -> Regex(pattern) } ?: VersionRegex.Defaults.none })
    }

    val patchRegex = objectFactory.property(Regex::class.java).apply {
        convention(fileConfig.map { it.patchRegex?.let { pattern -> Regex(pattern) } ?: VersionRegex.Defaults.patch })
    }

    val minorRegex = objectFactory.property(Regex::class.java).apply {
        convention(fileConfig.map { it.minorRegex?.let { pattern -> Regex(pattern) } ?: VersionRegex.Defaults.minor })
    }

    val majorRegex = objectFactory.property(Regex::class.java).apply {
        convention(fileConfig.map { it.majorRegex?.let { pattern -> Regex(pattern) } ?: VersionRegex.Defaults.major })
    }

    val core get() = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))

    fun lastVersionAndTag() = core.lastVersionAndTag()

    @Suppress("DEPRECATION")
    private fun resolveAllowDetachedHead(): Boolean = allowDetachedHeadProperty.orNull ?: disableDetached.get().let { shouldDisable -> !shouldDisable }

    fun calculateVersion() = core.calculateNextVersion(
        implicitPatch = implicitPatch.get(),
        versionRegex = versionRegex(),
        allowDetachedHead = resolveAllowDetachedHead(),
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

    private fun readTaggerFileFrom(directory: File): TaggerConfig? {
        val taggerFile = File(directory, ".tagger")
        return if (taggerFile.exists()) {
            try {
                Json.decodeFromString<TaggerConfig>(taggerFile.readText())
            } catch (e: Exception) {
                throw GradleException("Failed to parse .tagger file: ${e.message}", e)
            }
        } else {
            null
        }
    }
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
