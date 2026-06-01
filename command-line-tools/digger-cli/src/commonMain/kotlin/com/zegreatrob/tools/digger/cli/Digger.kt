package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.versionOption
import com.zegreatrob.tools.cli.loadHelpResource

class Digger : CliktCommand() {
    init {
        context {
            helpFormatter = { MordantMarkdownHelpFormatter(it, showDefaultValues = true) }
        }
        versionOption(Versions.diggerVersion)
    }

    override fun help(context: Context) = loadHelpResource("help/digger.md")

    override fun run() {
        echo("Welcome to Digger CLI.")
    }
}
