package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.zegreatrob.tools.tagger.guide.getTaggerGuideContent

class Guide : CliktCommand() {

    override fun help(context: Context) = getTaggerGuideContent()

    override fun run() {
        echo(currentContext.command.getFormattedHelp())
    }
}
