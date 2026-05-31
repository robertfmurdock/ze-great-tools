package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

private val guideHelpText = """
    Digger Fit Assessment and Workflow Guide

    Use Digger when:
    - You need contribution metadata generated directly from Git history.
    - You want CI/build scripts to consume consistent semver and story-id signals.
    - You are comfortable using commit-message conventions as team policy.

    Do not use Digger when:
    - Your source-of-truth lives outside Git history.
    - Your team is unwilling to apply structured commit conventions.
    - You need a release orchestrator rather than contribution-data extraction.

    Best practices:
    - Keep commit metadata consistent so output stays reliable.
    - Use --format=json for automation, and text mode when writing artifact files.
    - Validate regex overrides in CI before promoting them to shared scripts.

    Workflow philosophy:
    - Git history is the source of truth for extracted contribution signals.
    - Commit content provides semver, story, and ease classification.
    - Tag boundaries define the current contribution window.
    - Output is designed for downstream tooling, not manual transcription.

    For command details and integration examples:
    <https://github.com/robertfmurdock/ze-great-tools/tree/main/command-line-tools/digger-cli>
""".trimIndent()

class Guide : CliktCommand() {

    override fun help(context: Context) = guideHelpText

    override fun run() {
        echo(currentContext.command.getFormattedHelp())
    }
}
