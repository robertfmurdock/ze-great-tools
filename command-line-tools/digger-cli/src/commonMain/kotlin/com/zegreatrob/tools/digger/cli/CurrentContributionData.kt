package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.digger.core.DiggerCore
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.json.toJsonString
import kotlinx.serialization.json.Json

class CurrentContributionData : CliktCommand() {
    private val dir by argument("git-repo")
    private val outputFile by option().default("currentContributionData.json")
    private val label by option().default("")
    private val majorRegex by option()
    private val minorRegex by option()
    private val patchRegex by option()
    private val noneRegex by option()
    private val storyIdRegex by option()
    private val easeRegex by option()
    private val tagRegex by option()
    private val format by option("--format", help = "Output format: text (writes to file, default) or json (stdout)")
        .enum<OutputFormat> { it.name.lowercase() }
        .default(OutputFormat.TEXT)

    private val core
        get() = DiggerCore(
            label = label.ifBlank { dir.split("/").lastOrNull() },
            gitWrapper = GitAdapter(dir),
            messageDigger = MessageDigger(
                majorRegex = majorRegex?.let(::Regex) ?: MessageDigger.Defaults.majorRegex,
                minorRegex = minorRegex?.let(::Regex) ?: MessageDigger.Defaults.minorRegex,
                patchRegex = patchRegex?.let(::Regex) ?: MessageDigger.Defaults.patchRegex,
                noneRegex = noneRegex?.let(::Regex) ?: MessageDigger.Defaults.noneRegex,
                storyIdRegex = storyIdRegex?.let(::Regex) ?: MessageDigger.Defaults.storyIdRegex,
                easeRegex = easeRegex?.let(::Regex) ?: MessageDigger.Defaults.easeRegex,
            ),
            tagRegex = tagRegex?.let(::Regex) ?: DiggerCore.Defaults.tagRegex,
        )

    override fun help(context: Context) = $$"""
        $${super.help(context)}

        ## Output and Field Notes

        Fields in the resulting contribution object:

        - `storyId`: Story/ticket identifier extracted from commit messages.
        - `semver`: Highest semantic version impact in the contribution window (`major`, `minor`, `patch`, or `none`).
        - `ease`: Optional ease score extracted from commit messages.
        - `dateTime`, `firstCommitDateTime`, `tagDateTime`: ISO 8601 timestamps.

        ## Regex Customization

        Override parsing behavior with:
        `--major-regex`, `--minor-regex`, `--patch-regex`, `--none-regex`, `--story-id-regex`, `--ease-regex`, `--tag-regex`.

        ## CI Examples

        Extract story ID in a script:

        ```
        STORY_ID=$(digger current-contribution-data --format=json . | jq -r '.data.storyId')
        echo "story-id=$STORY_ID"
        ```
    """.trimIndent()

    override fun run() {
        val jsonString = core.currentContributionData().toJsonString()
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
