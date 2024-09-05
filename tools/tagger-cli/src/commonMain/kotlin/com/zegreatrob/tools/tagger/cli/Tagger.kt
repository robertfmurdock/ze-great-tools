package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Tagger : CliktCommand() {

    private val quiet by option("--quiet", "-q")
        .flag(default = false)

    override fun run() {
        if (!quiet) {
            echo("Welcome to Tagger CLI.")
        }
    }
}
