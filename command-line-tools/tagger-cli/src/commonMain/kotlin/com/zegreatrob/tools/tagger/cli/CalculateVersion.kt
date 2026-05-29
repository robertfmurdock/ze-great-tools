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
import com.github.ajalt.clikt.parameters.types.enum
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.SnapshotReason
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.VersionResult
import com.zegreatrob.tools.tagger.core.calculateNextVersion

enum class OutputFormat {
    TEXT,
    JSON,
}

class CalculateVersion : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val implicitPatch by option().boolean().default(true)
    private val disableDetachedDeprecated by option("--disable-detached", hidden = true).boolean()
    private val allowDetachedHead by option(
        "--allow-detached-head",
        help = "Allow version calculation in detached HEAD state. Detached HEAD is blocked by default as it can produce unreliable version calculations.",
    ).boolean()
    private val disableDetached get() = allowDetachedHead?.let { !it } ?: disableDetachedDeprecated ?: true
    private val forceSnapshot by option(
        help = "Force -SNAPSHOT suffix on version, overriding normal release conditions. Use for testing or CI workflows that require snapshot versions.",
    ).boolean().default(false)
    private val releaseBranch by option(
        help = "Specify release branch name. Versions are release versions only on this branch; other branches produce -SNAPSHOT versions.",
    )
    private val format by option(
        "--format",
        help = "Output format (default: text). Use json for structured data with version, snapshot status, and diagnostic flags.",
    ).enum<OutputFormat> { it.name.lowercase() }
        .default(OutputFormat.TEXT)
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
                    is VersionResult.Success -> when (format) {
                        OutputFormat.JSON -> outputJson(version = version, snapshotReasons = snapshotReasons, warnings = warnings)
                        OutputFormat.TEXT -> output(message = version, errorMessage = snapshotReasons, warnings = warnings)
                    }

                    is VersionResult.Failure -> when (format) {
                        OutputFormat.JSON -> {
                            echo(errorResponse(message, "CONFIGURATION_ERROR"))
                            throw CliktError("", printError = false, statusCode = 1)
                        }

                        OutputFormat.TEXT -> throw CliktError(message)
                    }
                }
            }
    }

    private fun output(
        message: String,
        errorMessage: List<SnapshotReason>,
        warnings: List<String>,
    ) {
        echo(message)
        if (errorMessage.isNotEmpty()) {
            errorMessage.forEach { reason ->
                echo("${reason.name} - ${reason.message}", err = true)
            }
        }
        warnings.forEach { echo(it, err = true) }
    }

    private fun outputJson(
        version: String,
        snapshotReasons: List<SnapshotReason>,
        warnings: List<String>,
    ) {
        val isSnapshot = version.endsWith("-SNAPSHOT")
        val jsonOutput = versionSuccessResponse(
            VersionData(
                version = version,
                snapshot = isSnapshot,
                snapshotReasons = snapshotReasons.map { it.toString() },
                warnings = warnings,
            ),
        )
        echo(jsonOutput)
    }

    private fun versionRegex() = VersionRegex(
        none = Regex(noneRegex, RegexOption.IGNORE_CASE),
        patch = Regex(patchRegex, RegexOption.IGNORE_CASE),
        minor = Regex(minorRegex, RegexOption.IGNORE_CASE),
        major = Regex(majorRegex, RegexOption.IGNORE_CASE),
        unified = versionRegex?.let { Regex(it, RegexOption.IGNORE_CASE) },
    )
}
