package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand

class Welcome : CliktCommand() {

    override fun run() {
        echo("Welcome to Tagger CLI.")
    }
}
