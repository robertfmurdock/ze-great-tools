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
        ${fitCheckSection()}

        ${workflowSection()}

        ${antiPatternSection()}

        ${configurationSection()}

        ${outputSection()}

        ${automationSection()}
    """.trimIndent()

    private fun fitCheckSection() = """
        Use Tagger when:
          • Version numbers should live on Git tags as the source of truth
          • Commit content should determine the next version
          • You can enforce full Git history/tags in CI

        Do not use Tagger when:
          • Version truth must come from artifact repos or build metadata
          • You need multi-stream version lines as a first-class workflow
          • You cannot enforce Git/CI prerequisites (full history, tags, branch context)

        For full rationale: docs/why-tagger.md
    """.trimIndent()

    private fun workflowSection() = """
        Quick start (default safe path):
          1. tagger calculate-version --format=json
          2. Check that snapshot == false (if true, fix conditions before tagging)
          3. tagger tag --version <calculated>

        The tag command expects the output of calculate-version.
        Manual version override is for controlled exceptions only.
    """.trimIndent()

    private fun antiPatternSection() = """
        Do:
          • Calculate then tag (two-step workflow)
          • Run with full Git history/tags in CI
          • Use calculated version as tag input

        Don't:
          • Tag arbitrary versions from local assumptions
          • Run with shallow/partial checkout when calculating release versions
          • Override computed versions casually
    """.trimIndent()

    private fun configurationSection() = """
        Configuration:
          Settings can be stored in a .tagger JSON file at the repository root.
          This eliminates the need to pass common options on every invocation.

          Generate a template: tagger generate-settings-file
          Edit the file to set defaults for options like release-branch, regex patterns, etc.

          Command-line options override .tagger file settings.
    """.trimIndent()

    private fun outputSection() = """
        Output:
          Text format writes version to stdout, diagnostics to stderr.
          Command substitution captures only stdout: VERSION=$$(tagger -q calculate-version ...)

          -SNAPSHOT suffix indicates unmet conditions for release.
          Snapshot versions should not be used in releases or tags.
          After 'tagger tag', subsequent calculate-version returns the bare version.

          Snapshot reasons (on stderr) describe conditions that must be resolved:
            DIRTY               - uncommitted changes in working directory
            AHEAD               - local branch ahead of remote
            BEHIND              - local branch behind remote
            NOT_RELEASE_BRANCH  - not on configured release branch
            NO_NEW_VERSION      - no new commits since last tag
            FORCED              - --force-snapshot=true was used

          Resolving snapshot reasons:
            DIRTY              → commit/stash changes
            AHEAD/BEHIND       → sync with remote (push/pull)
            NOT_RELEASE_BRANCH → switch to release branch or configure .tagger
            NO_NEW_VERSION     → add explicit semver signal commit ([major], [minor], [patch])
            FORCED             → remove --force-snapshot flag in release flows
    """.trimIndent()

    private fun automationSection() = """
        Automation & AI Agents:
          Use --format=json for structured data with machine-readable fields:
            - 'snapshot' boolean indicating tagging readiness
            - 'snapshotReasons' array listing specific conditions
            - 'version' string for consistent parsing

          Example: tagger calculate-version --format=json

          See subcommand help for details: tagger calculate-version --help
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
