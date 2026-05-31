package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.versionOption

class Digger : CliktCommand() {
    init {
        context {
            helpFormatter = { MordantMarkdownHelpFormatter(it, showDefaultValues = true) }
        }
        versionOption(Versions.diggerVersion)
    }

    override fun help(context: Context) = HELP_TEXT

    override fun run() {
        echo("Welcome to Digger CLI.")
    }

    companion object {
        private val HELP_TEXT = """
            Digger extracts contribution metadata from Git history for CI and reporting workflows.
            **Use `--format=json` for automation** and text mode when writing output files.

            Typical CI/build script usage:

            ```
            digger current-contribution-data --format=json path/to/repo
            digger all-contribution-data --format=json path/to/repo
            ```

            | Command | Purpose |
            | --- | --- |
            | `current-contribution-data` | Data since the latest version tag |
            | `all-contribution-data` | Data grouped across all tagged contribution windows |
            | `guide` | Fit-check, best practices, and workflow philosophy |

            For fit assessment and philosophy: digger guide
        """.trimIndent()
    }
}
