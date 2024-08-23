package com.zegreatrob.tools.cli

import java.io.File

actual fun String.writeToFile(outputFile: String) = File(outputFile).writeText(this)
