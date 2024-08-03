package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Welcome()
    .subcommands(CurrentContributionData())
    .main(args)
