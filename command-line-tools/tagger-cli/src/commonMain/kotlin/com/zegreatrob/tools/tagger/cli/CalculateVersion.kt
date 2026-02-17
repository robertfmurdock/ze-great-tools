package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.SnapshotReason
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.VersionResult
import com.zegreatrob.tools.tagger.core.calculateNextVersion

class CalculateVersion : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val implicitPatch by option().boolean().default(true)
    private val disableDetached by option().boolean().default(true)
    private val forceSnapshot by option().boolean().default(false)
    private val releaseBranch by option()
    private val majorRegex by option().default(VersionRegex.Defaults.major.pattern)
    private val minorRegex by option().default(VersionRegex.Defaults.minor.pattern)
    private val patchRegex by option().default(VersionRegex.Defaults.patch.pattern)
    private val noneRegex by option().default(VersionRegex.Defaults.none.pattern)
    private val versionRegex by option().check(
        message = VersionRegex.MISSING_GROUP_ERROR,
        validator = VersionRegex.Companion::containsAllGroups,
    )

    override fun run() {
        TaggerCore(GitAdapter(workingDirectory))
            .calculateNextVersion(
                implicitPatch = implicitPatch,
                disableDetached = disableDetached,
                versionRegex = versionRegex(),
                forceSnapshot = forceSnapshot,
                releaseBranch = releaseBranch ?: "",
            )
            .run {
                when (this) {
                    is VersionResult.Success -> output(message = version, errorMessage = snapshotReasons)
                    is VersionResult.Failure -> throw CliktError(message)
                }
            }
    }

    private fun output(
        message: String,
        errorMessage: List<SnapshotReason>,
    ) {
        echo(message)
        if (errorMessage.isNotEmpty()) {
            echo(errorMessage, err = true)
        }
    }

    private fun versionRegex() = VersionRegex(
        none = Regex(noneRegex, RegexOption.IGNORE_CASE),
        patch = Regex(patchRegex, RegexOption.IGNORE_CASE),
        minor = Regex(minorRegex, RegexOption.IGNORE_CASE),
        major = Regex(majorRegex, RegexOption.IGNORE_CASE),
        unified = versionRegex?.let { Regex(it, RegexOption.IGNORE_CASE) },
    )
}
