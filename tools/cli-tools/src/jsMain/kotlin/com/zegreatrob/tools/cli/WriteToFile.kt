package com.zegreatrob.tools.cli

private val fs = js("require('node:fs')")
actual fun String.writeToFile(outputFile: String) {
    fs.writeFileSync(outputFile, this)
}
