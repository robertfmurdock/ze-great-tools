package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption

class Tagger : CliktCommand() {

    init {
        versionOption(Versions.taggerVersion)
    }

    private val quiet by option("--quiet", "-q")
        .flag(default = false)

    override fun run() {
        if (!quiet) {
            echo("Welcome to Tagger CLI.")
        }
    }
}
