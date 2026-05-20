package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TagResult
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.VersionResult
import com.zegreatrob.tools.tagger.core.calculateNextVersion
import com.zegreatrob.tools.tagger.core.tag

class Tag : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val releaseBranch by option().required()
    private val versionOption: String? by option("--version")
    private val userName: String? by option()
    private val userEmail: String? by option()
    private val warningsAsErrors by option().boolean().default(false)
    private val format by option("--format", help = "Output format: text (default) or json")
        .enum<OutputFormat> { it.name.lowercase() }
        .default(OutputFormat.TEXT)
    override fun run() {
        val taggerCore = TaggerCore(GitAdapter(workingDirectory))
        val version = versionOption ?: calculateVersion(taggerCore)
        taggerCore
            .tag(version, releaseBranch, userName, userEmail)
            .let {
                when (it) {
                    TagResult.Success -> when (format) {
                        OutputFormat.JSON -> echo(tagSuccessResponse(TagData(tag = version)))
                        OutputFormat.TEXT -> echo("Success!")
                    }

                    is TagResult.Error -> when (format) {
                        OutputFormat.JSON -> {
                            echo(errorResponse(it.message, "TAG_ERROR"))
                            throw CliktError("", printError = false, statusCode = if (warningsAsErrors) 1 else 0)
                        }

                        OutputFormat.TEXT -> if (warningsAsErrors) {
                            throw CliktError(it.message)
                        } else {
                            echo(it.message, err = true)
                        }
                    }
                }
            }
    }

    private fun calculateVersion(taggerCore: TaggerCore): String {
        val versionResult = taggerCore.calculateNextVersion(
            implicitPatch = true,
            disableDetached = true,
            versionRegex = VersionRegex(
                none = VersionRegex.Defaults.none,
                patch = VersionRegex.Defaults.patch,
                minor = VersionRegex.Defaults.minor,
                major = VersionRegex.Defaults.major,
                unified = null,
            ),
            forceSnapshot = false,
            releaseBranch = releaseBranch,
        )
        return when (versionResult) {
            is VersionResult.Success -> versionResult.version.removeSuffix("-SNAPSHOT")
            is VersionResult.Failure -> throw CliktError(versionResult.message)
        }
    }
}
