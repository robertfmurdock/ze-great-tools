package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand

class Welcome : CliktCommand() {

    override fun run() {
        echo("Welcome to Digger CLI.")
    }
}
