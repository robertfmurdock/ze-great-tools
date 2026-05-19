package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.json.toJsonString
import kotlinx.serialization.json.Json

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
    private val formatString by option("--format").default("text")
    private val format: OutputFormat
        get() = try {
            OutputFormat.fromString(formatString)
        } catch (e: IllegalArgumentException) {
            throw CliktError(e.message ?: "Invalid format")
        }

    private val core
        get() = DiggerCore(
            label = label.ifBlank { dir.split("/").lastOrNull() },
            tagRegex = tagRegex?.let(::Regex) ?: DiggerCore.Defaults.tagRegex,
            gitWrapper = GitAdapter(dir),
            messageDigger = MessageDigger(
                majorRegex = majorRegex?.let(::Regex) ?: MessageDigger.Defaults.majorRegex,
                minorRegex = minorRegex?.let(::Regex) ?: MessageDigger.Defaults.minorRegex,
                patchRegex = patchRegex?.let(::Regex) ?: MessageDigger.Defaults.patchRegex,
                noneRegex = noneRegex?.let(::Regex) ?: MessageDigger.Defaults.noneRegex,
                storyIdRegex = storyIdRegex?.let(::Regex) ?: MessageDigger.Defaults.storyIdRegex,
                easeRegex = easeRegex?.let(::Regex) ?: MessageDigger.Defaults.easeRegex,
            ),
        )

    override fun run() {
        val jsonString = core.allContributionData().toJsonString()
        when (format) {
            OutputFormat.JSON -> {
                val dataElement = Json.parseToJsonElement(jsonString)
                echo(successResponse(dataElement))
            }

            OutputFormat.TEXT -> {
                jsonString
                    .writeToFile(outputFile)
                    .also { echo("Data written to $outputFile") }
            }
        }
    }
}
