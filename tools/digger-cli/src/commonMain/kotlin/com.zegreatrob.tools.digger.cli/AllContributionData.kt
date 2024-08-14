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
    private val dir by argument("git-repo")
    private val outputFile by option().default("allContributionData.json")
    private val label by option().default("")
    private val majorRegex by option()
    private val minorRegex by option()
    private val patchRegex by option()
    private val noneRegex by option()
    private val storyIdRegex by option()
    private val easeRegex by option()
    private val tagRegex by option()

    private val core
        get() = DiggerCore(
            label = label.ifBlank { dir.split("/").lastOrNull() },
            tagRegex = tagRegex?.let(::Regex) ?: DiggerCore.Defaults.tagRegex,
            gitWrapper = DiggerGitWrapper(dir),
            messageDigger = MessageDigger(
                majorRegex = majorRegex?.let(::Regex) ?: MessageDigger.Defaults.majorRegex,
                minorRegex = minorRegex?.let(::Regex) ?: MessageDigger.Defaults.minorRegex,
                patchRegex = patchRegex?.let(::Regex) ?: MessageDigger.Defaults.patchRegex,
                noneRegex = noneRegex?.let(::Regex) ?: MessageDigger.Defaults.noneRegex,
                storyIdRegex = storyIdRegex?.let(::Regex) ?: MessageDigger.Defaults.storyIdRegex,
                easeRegex = easeRegex?.let(::Regex) ?: MessageDigger.Defaults.easeRegex,
            ),
        )

    override fun run() = core.allContributionData()
        .toJsonString()
        .writeToFile(outputFile)
        .also { echo("Data written to $outputFile") }
}
