package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.VersionResult
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

abstract class CalculateVersion : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workingDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val releaseBranch: Property<String>

    @get:Input
    abstract val implicitPatch: Property<Boolean>

    @get:Input
    abstract val disableDetached: Property<Boolean>

    @get:Input
    abstract val forceSnapshot: Property<Boolean>

    @get:Input
    abstract val noneRegex: Property<Regex>

    @get:Input
    abstract val patchRegex: Property<Regex>

    @get:Input
    abstract val minorRegex: Property<Regex>

    @get:Input
    abstract val majorRegex: Property<Regex>

    @get:Input
    @get:Optional
    abstract val versionRegex: Property<Regex>

    @get:Input
    abstract val exportToGithubEnv: Property<Boolean>

    @TaskAction
    fun execute() = when (val result = calculateVersion()) {
        is VersionResult.Success -> result.outputSuccess()
        is VersionResult.Failure -> throw GradleException(result.message)
    }

    private fun calculateVersion(): VersionResult {
        val core = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))
        return core.calculateNextVersion(
            implicitPatch = implicitPatch.get(),
            versionRegex = VersionRegex(
                none = noneRegex.get(),
                patch = patchRegex.get(),
                minor = minorRegex.get(),
                major = majorRegex.get(),
                unified = versionRegex.orNull?.also { it.validateVersionRegex() },
            ),
            disableDetached = disableDetached.get(),
            forceSnapshot = forceSnapshot.get(),
            releaseBranch = releaseBranch.orNull
                ?: throw GradleException("Please configure the tagger release branch."),
        )
    }

    private fun VersionResult.Success.outputSuccess() {
        logger.quiet(version)
        if (snapshotReasons.isNotEmpty()) {
            System.err.println(snapshotReasons.joinToString(","))
        }
        val githubEnvFile = System.getenv("GITHUB_ENV")
        if (exportToGithubEnv.get() && githubEnvFile != null) {
            FileOutputStream(githubEnvFile, true).write("TAGGER_VERSION=$version".toByteArray())
        }
    }
}
