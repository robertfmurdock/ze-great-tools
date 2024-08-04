package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.json.toJsonString

class AllContributionData : CliktCommand() {
    private val dir by argument("dir")
    private val label by option().default("")

    private val core get() = DiggerCore(
        label = label.ifBlank { dir.split("/").lastOrNull() },
        gitWrapper = DiggerGitWrapper(dir),
        messageDigger = MessageDigger(),
    )

    override fun run() {
        core.allContributionData()
            .toJsonString()
            .let(::echo)
    }
}
