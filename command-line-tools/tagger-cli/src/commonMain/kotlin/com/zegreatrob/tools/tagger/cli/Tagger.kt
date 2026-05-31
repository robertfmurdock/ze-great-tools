package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption

class Tagger : CliktCommand() {

    init {
        context {
            helpFormatter = { MordantMarkdownHelpFormatter(it, showDefaultValues = true) }
        }
        versionOption(Versions.taggerVersion)
    }

    override fun help(context: Context) = """
        Tagger calculates semantic versions from Git history and enforces tagging policy.
        Version numbers live on Git tags. Commit content determines the next version.

        Typical CI/build script usage:

        ```
        VERSION=${'$'}(tagger calculate-version)
        ./your-build-script.sh version=${'$'}VERSION
        tagger tag --version ${'$'}VERSION
        ```

        Use --format=json for machine-readable output. Build with calculated version before tagging.
        For fit assessment and philosophy: tagger guide
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
