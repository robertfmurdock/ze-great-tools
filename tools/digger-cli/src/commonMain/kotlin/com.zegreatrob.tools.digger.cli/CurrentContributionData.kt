package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.json.toJsonString

class CurrentContributionData : CliktCommand() {
    private val dir by argument("git-repo")
    private val outputFile by option().default("currentContributionData.json")
    private val label by option().default("")
    private val majorRegex by option()
    private val minorRegex by option()
    private val patchRegex by option()
    private val noneRegex by option()

    private val core
        get() = DiggerCore(
            label = label.ifBlank { dir.split("/").lastOrNull() },
            gitWrapper = DiggerGitWrapper(dir),
            messageDigger = MessageDigger(
                majorRegex = majorRegex?.let(::Regex) ?: MessageDigger.Defaults.majorRegex,
                minorRegex = minorRegex?.let(::Regex) ?: MessageDigger.Defaults.minorRegex,
                patchRegex = patchRegex?.let(::Regex) ?: MessageDigger.Defaults.patchRegex,
                noneRegex = noneRegex?.let(::Regex) ?: MessageDigger.Defaults.noneRegex,
            ),
        )

    override fun run() = core.currentContributionData()
        .toJsonString()
        .writeToFile(outputFile)
        .also { echo("Data written to $outputFile") }
}

expect fun String.writeToFile(outputFile: String)
