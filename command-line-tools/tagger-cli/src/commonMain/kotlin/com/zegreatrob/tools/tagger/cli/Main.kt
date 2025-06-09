package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.zegreatrob.tools.cli.platformArgCorrection

fun main(args: Array<String>) = cli()
    .main(platformArgCorrection(args))

fun cli() = Tagger()
    .subcommands(CalculateVersion())
    .subcommands(Tag())
    .subcommands(GenerateSettingsFile())
