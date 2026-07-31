package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.zegreatrob.tools.cli.loadHelpResource

data class DiggerContext(val showCommands: Boolean)

class Digger : CliktCommand() {
    init {
        context {
            helpFormatter = { MordantMarkdownHelpFormatter(it, showDefaultValues = true) }
        }
        versionOption(Versions.diggerVersion)
    }

    override fun help(context: Context) = loadHelpResource("help/digger.md")

    private val showCommands by option(
        "--show-commands",
        help = "Print git commands to stderr before executing them for transparency and audit purposes.",
    ).flag(default = false)

    override fun run() {
        currentContext.findOrSetObject { DiggerContext(showCommands = showCommands) }
        echo("Welcome to Digger CLI.")
    }
}
