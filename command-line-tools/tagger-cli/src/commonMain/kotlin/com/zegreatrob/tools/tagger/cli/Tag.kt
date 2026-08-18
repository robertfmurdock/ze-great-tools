package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.cli.loadHelpResource
import com.zegreatrob.tools.tagger.core.TagResult
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.tag

class Tag : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    override fun help(context: Context) = "${loadHelpResource("help/tag.md")}\n\n${configFileHelpSuffix()}".trim()

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val releaseBranch by option(
        help = "Release branch name. Required unless provided in .tagger config file.",
    ).required()
    private val version: String by option("--version", help = "Version to tag (required)").required()
    private val userName: String? by option()
    private val userEmail: String? by option()
    private val allowDetachedHead by option("--allow-detached-head").boolean().default(false)
    private val dryRun by option("--dry-run").boolean().default(false)
    private val warningsAsErrors by option().boolean().default(false)
    private val format by option("--format", help = "Output format for result")
        .enum<OutputFormat> { it.name.lowercase() }
        .default(OutputFormat.TEXT, defaultForHelp = "text")

    override fun run() {
        val gitAdapter = GitAdapter(workingDirectory, commandLogger = commandLogger())
        if (dryRun) showDryRun(gitAdapter) else handle(tag(gitAdapter))
    }

    private fun commandLogger(): ((String) -> Unit)? = if (currentContext.findObject<TaggerContext>()?.showCommands == true) {
        { command: String -> echo(command, err = true) }
    } else {
        null
    }

    private fun showDryRun(gitAdapter: GitAdapter) {
        val headCommit = gitAdapter.headCommitId()
        val headBranch = gitAdapter.status().head
        echo("Would create annotated tag '$version' at $headCommit on branch '$headBranch'.")
        echo("Would push to remote 'origin'.")
        echo("(no changes made)")
    }

    private fun tag(gitAdapter: GitAdapter) = TaggerCore(gitAdapter)
        .tag(version, releaseBranch, userName, userEmail, allowDetachedHead)

    private fun handle(result: TagResult) = when (result) {
        TagResult.Success -> handleSuccess()
        is TagResult.Warning -> handleWarning(result.message)
        is TagResult.Failure -> handleFailure(result.message)
    }

    private fun handleSuccess() = when (format) {
        OutputFormat.JSON -> echo(tagSuccessResponse(TagData(tag = version)))
        OutputFormat.TEXT -> echo("Success!")
    }

    private fun handleWarning(message: String) = when (format) {
        OutputFormat.JSON -> {
            echo(errorResponse(message, "TAG_ERROR"))
            throw silentError(if (warningsAsErrors) 1 else 0)
        }

        OutputFormat.TEXT -> if (warningsAsErrors) throw CliktError(message) else echo(message, err = true)
    }

    private fun handleFailure(message: String): Nothing = when (format) {
        OutputFormat.JSON -> {
            echo(errorResponse(message, "TAG_ERROR"))
            throw silentError(1)
        }

        OutputFormat.TEXT -> throw CliktError(message)
    }

    private fun silentError(statusCode: Int) = CliktError("", printError = false, statusCode = statusCode)
}
