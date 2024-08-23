package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.subcommands
import com.zegreatrob.tools.cli.platformArgCorrection

fun main(args: Array<String>) = Welcome()
    .subcommands(CalculateVersion())
    .main(platformArgCorrection(args))
