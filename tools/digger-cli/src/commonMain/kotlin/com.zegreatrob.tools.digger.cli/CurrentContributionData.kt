package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.json.toJsonString

class CurrentContributionData : CliktCommand() {
    private val dir by option().default("")
    private val label by option().default("")

    private val core get() = DiggerCore(label.ifBlank { null }, DiggerGitWrapper(dir), MessageDigger())

    override fun run() = core.currentContributionData()
        .toJsonString()
        .let(::echo)
}
