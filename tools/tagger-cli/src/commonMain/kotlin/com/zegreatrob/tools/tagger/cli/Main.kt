package com.zegreatrob.tools.tagger.cli

import com.zegreatrob.tools.cli.platformArgCorrection

fun main(args: Array<String>) = Welcome()
    .main(platformArgCorrection(args))
