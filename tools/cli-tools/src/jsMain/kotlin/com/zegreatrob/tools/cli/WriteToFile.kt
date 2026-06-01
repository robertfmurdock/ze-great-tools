package com.zegreatrob.tools.cli

actual fun String.writeToFile(outputFile: String) {
    NodeFs.writeFileSync(outputFile, this)
}
