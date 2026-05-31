package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context

class Guide : CliktCommand() {

    override fun help(context: Context) = """
        # Tagger Fit Assessment & Workflow Guide

        ## Use Tagger when:
        - You want deterministic versioning behavior from Git history
        - You're willing to use Git tags as version source-of-truth
        - You can enforce CI prerequisites consistently (full history, tags, branch context)

        ## Do not use Tagger when:
        - You want version truth from an artifact repository or build metadata instead of Git tags
        - You need multi-stream version lines as a first-class workflow
        - You want a single atomic command to orchestrate the entire release lifecycle
        - You are not willing to enforce Git/CI prerequisites

        ## Best Practices

        **Do:** Keep version decisions explicit and reversible. Calculate first, review output, then tag.

        **Don't:** Override calculated versions casually without understanding why the calculation differs.

        **Don't:** Skip CI prerequisites (full history, branch context) to "make it work" in your pipeline.

        ## Workflow Philosophy

        - Version numbers live on annotated Git tags.
        - Commit content determines the next version.
        - Releases should be created on a release branch.
        - Version calculation happens separately from release (`calculate-version` → review → `tag`).
        - Configuration lives in code and is governed the same way.

        ---

        For more details on Tagger principles, tradeoffs, and evaluation criteria:
        <https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/why-tagger.md>
    """.trimIndent()

    override fun run() {
        echo(currentContext.command.getFormattedHelp())
    }
}
