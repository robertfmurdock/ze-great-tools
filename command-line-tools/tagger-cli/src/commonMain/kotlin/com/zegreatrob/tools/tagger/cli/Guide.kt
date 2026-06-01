package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.zegreatrob.tools.cli.loadHelpResource

class Guide : CliktCommand() {

    override fun help(context: Context) = loadHelpResource("help/tagger-guide.md")

    override fun run() {
        echo(currentContext.command.getFormattedHelp())
    }
}
