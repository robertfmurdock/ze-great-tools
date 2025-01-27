package com.zegreatrob.tools.cli

@JsModule("node:fs")
@JsNonModule
private external val fs: dynamic

actual fun String.writeToFile(outputFile: String) {
    fs.writeFileSync(outputFile, this)
}
