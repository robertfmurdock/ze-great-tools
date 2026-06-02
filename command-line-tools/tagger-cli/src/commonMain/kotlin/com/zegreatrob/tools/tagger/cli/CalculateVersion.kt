package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.cli.loadHelpResource
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.tagger.core.SnapshotReason
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.VersionResult
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.json.Json

enum class OutputFormat {
    TEXT,
    JSON,
}

class CalculateVersion : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    override fun help(context: Context) = "${loadHelpResource("help/calculate-version.md")}\n\n${configFileHelpSuffix()}".trim()

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val implicitPatch by option(
        help = "Automatically bump patch version when no version-tagged commits exist since last release.",
    ).boolean().default(true)
    private val allowDetachedHeadOption by option(
        "--allow-detached-head",
        help = "Allow version calculation in detached HEAD state. Detached HEAD is blocked by default as it can produce unreliable version calculations.",
    ).boolean()
    private val allowDetachedHead get() = allowDetachedHeadOption ?: false
    private val forceSnapshot by option(
        help = "Force -SNAPSHOT suffix on version, overriding normal release conditions. Use for testing or CI workflows that require snapshot versions.",
    ).boolean().default(false)
    private val releaseBranch by option(
        help = "Specify release branch name. Versions are release versions only on this branch; other branches produce -SNAPSHOT versions.",
    )
    private val format by option(
        "--format",
        help = "Use json for structured data with version, snapshot status, and diagnostic flags.",
    ).enum<OutputFormat> { it.name.lowercase() }
        .default(OutputFormat.TEXT, defaultForHelp = "text")
    private val warningsAsErrors by option().boolean().default(false)
    private val majorRegex by option().default(VersionRegex.Defaults.major.pattern)
    private val minorRegex by option().default(VersionRegex.Defaults.minor.pattern)
    private val patchRegex by option().default(VersionRegex.Defaults.patch.pattern)
    private val noneRegex by option().default(VersionRegex.Defaults.none.pattern)
    private val versionRegex by option().check(
        message = VersionRegex.MISSING_GROUP_ERROR,
        validator = VersionRegex.Companion::containsAllGroups,
    )

    override fun run() {
        val result = TaggerCore(GitAdapter(workingDirectory))
            .calculateNextVersion(
                implicitPatch = implicitPatch,
                allowDetachedHead = allowDetachedHead,
                versionRegex = buildVersionRegex(),
                forceSnapshot = forceSnapshot,
                releaseBranch = releaseBranch ?: "",
            )
        handleResult(result)
    }

    private fun handleResult(result: VersionResult) {
        when (result) {
            is VersionResult.Success -> handleSuccess(result)
            is VersionResult.Failure -> handleFailure(result)
        }
    }

    private fun handleSuccess(result: VersionResult.Success) {
        val shouldEscalate = warningsAsErrors && result.warnings.isNotEmpty()
        when (format) {
            OutputFormat.JSON -> handleSuccessJson(result, shouldEscalate)
            OutputFormat.TEXT -> handleSuccessText(result)
        }
        if (shouldEscalate) {
            throw CliktError("", printError = false, statusCode = 1)
        }
    }

    private fun handleSuccessJson(result: VersionResult.Success, shouldEscalate: Boolean) {
        if (shouldEscalate) {
            echo(errorResponse("Warnings escalated to errors: ${result.warnings.joinToString("; ")}", "WARNINGS_AS_ERRORS"))
        } else {
            outputJson(version = result.version, snapshotReasons = result.snapshotReasons, warnings = result.warnings)
        }
    }

    private fun handleSuccessText(result: VersionResult.Success) {
        output(message = result.version, errorMessage = result.snapshotReasons, warnings = result.warnings)
    }

    private fun handleFailure(result: VersionResult.Failure) {
        when (format) {
            OutputFormat.JSON -> handleFailureJson(result)
            OutputFormat.TEXT -> handleFailureText(result)
        }
    }

    private fun handleFailureJson(result: VersionResult.Failure) {
        echo(errorResponse(result.message, "CONFIGURATION_ERROR"))
        throw CliktError("", printError = false, statusCode = 1)
    }

    private fun handleFailureText(result: VersionResult.Failure): Unit = throw CliktError(result.message)

    private fun output(
        message: String,
        errorMessage: List<SnapshotReason>,
        warnings: List<String>,
    ) {
        echo(message)
        if (errorMessage.isNotEmpty()) {
            errorMessage.forEach { echo(formatSnapshotReason(it), err = true) }
        }
        warnings.forEach { echo(it, err = true) }
    }

    private fun formatSnapshotReason(reason: SnapshotReason) = "${reason.name} - ${reason.message}"

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

    private fun buildVersionRegex() = VersionRegex(
        none = Regex(noneRegex, RegexOption.IGNORE_CASE),
        patch = Regex(patchRegex, RegexOption.IGNORE_CASE),
        minor = Regex(minorRegex, RegexOption.IGNORE_CASE),
        major = Regex(majorRegex, RegexOption.IGNORE_CASE),
        unified = versionRegex?.let { Regex(it, RegexOption.IGNORE_CASE) },
    )
}
