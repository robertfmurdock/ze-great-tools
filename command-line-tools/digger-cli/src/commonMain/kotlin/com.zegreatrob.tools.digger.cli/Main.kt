package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.zegreatrob.tools.cli.platformArgCorrection

fun main(args: Array<String>) = cli()
    .main(platformArgCorrection(args))

fun cli() = Digger()
    .subcommands(CurrentContributionData())
    .subcommands(AllContributionData())
