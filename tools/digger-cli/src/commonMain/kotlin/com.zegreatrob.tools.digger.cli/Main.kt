package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.subcommands
import com.zegreatrob.tools.cli.platformArgCorrection

fun main(args: Array<String>) = Welcome()
    .subcommands(CurrentContributionData())
    .subcommands(AllContributionData())
    .main(platformArgCorrection(args))
