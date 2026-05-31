package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption

class Tagger : CliktCommand() {

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }
        versionOption(Versions.taggerVersion)
    }

    override fun help(context: Context) = """
        Tagger calculates semantic versions from Git history and enforces tagging policy.
        Version numbers live on Git tags. Commit content determines the next version.

        Quick start: tagger calculate-version → check snapshot → tagger tag --version <result>
        For fit assessment and philosophy: tagger guide

        Automation & AI Agents:
          Use --format=json for structured data with machine-readable fields:
            - 'snapshot' boolean indicating tagging readiness
            - 'snapshotReasons' array listing unmet conditions
            - 'version' string for consistent parsing

          -SNAPSHOT suffix means unmet conditions; should not be used in releases or tags.

          Example: tagger calculate-version --format=json
    """.trimIndent()

    private val quiet by option(
        "--quiet",
        "-q",
        help = "Suppress welcome message. Version goes to stdout, diagnostics to stderr " +
            "(safe for: VERSION=$$(tagger -q ...))",
    ).flag(default = false)

    override fun run() {
        if (!quiet && currentContext.invokedSubcommand == null) {
            echo("Welcome to Tagger CLI.")
        }
    }
}
