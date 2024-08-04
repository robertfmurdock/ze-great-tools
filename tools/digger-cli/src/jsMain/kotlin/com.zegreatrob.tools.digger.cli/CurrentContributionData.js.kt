package com.zegreatrob.tools.digger.cli

private val fs = js("require('node:fs')")

actual fun String.writeToFile(outputFile: String) {
    fs.writeFileSync(outputFile, this)
}
