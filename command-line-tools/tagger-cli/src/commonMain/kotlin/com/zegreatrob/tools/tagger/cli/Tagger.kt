package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption

class Tagger : CliktCommand() {

    override fun help(context: Context) = """
        Output:
          Text format writes version to stdout, diagnostics to stderr.
          Command substitution captures only stdout: VERSION=${'$'}(tagger -q calculate-version ...)

          -SNAPSHOT suffix indicates unmet conditions for tagging (not decorative text).
          The version is ready to tag only when -SNAPSHOT is absent.
          After 'tagger tag', subsequent calculate-version returns the bare version.

          Snapshot reasons (on stderr) describe conditions that must be resolved:
            DIRTY               - uncommitted changes in working directory
            AHEAD               - local branch ahead of remote
            BEHIND              - local branch behind remote
            NOT_RELEASE_BRANCH  - not on configured release branch
            NO_NEW_VERSION      - no new commits since last tag
            FORCED              - --force-snapshot=true was used

        Automation & AI Agents:
          Use --format=json for structured data with machine-readable fields:
            - 'snapshot' boolean indicating tagging readiness
            - 'snapshotReasons' array listing specific conditions
            - 'version' string for consistent parsing

          Example: tagger calculate-version --format=json

          See subcommand help for details: tagger calculate-version --help
    """.trimIndent()

    init {
        versionOption(Versions.taggerVersion)
    }

    private val quiet by option(
        "--quiet",
        "-q",
        help = "Suppress welcome message. Version goes to stdout, diagnostics to stderr " +
            "(safe for: VERSION=\$(tagger -q ...))",
    ).flag(default = false)

    override fun run() {
        if (!quiet && currentContext.invokedSubcommand == null) {
            echo("Welcome to Tagger CLI.")
        }
    }
}
