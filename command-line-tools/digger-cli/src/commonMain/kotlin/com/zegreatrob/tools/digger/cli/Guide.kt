package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.zegreatrob.tools.digger.guide.getDiggerGuideContent

class Guide : CliktCommand() {

    override fun help(context: Context) = getDiggerGuideContent()

    override fun run() {
        echo(currentContext.command.getFormattedHelp())
    }
}
