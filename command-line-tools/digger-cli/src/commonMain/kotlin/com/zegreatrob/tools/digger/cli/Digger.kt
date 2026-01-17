package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.versionOption

class Digger : CliktCommand() {
    init {
        versionOption(Versions.diggerVersion)
    }

    override fun run() {
        echo("Welcome to Digger CLI.")
    }
}
