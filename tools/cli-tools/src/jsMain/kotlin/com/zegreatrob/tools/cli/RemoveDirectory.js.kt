package com.zegreatrob.tools.cli

import kotlin.js.json

private val fs = js("require('node:fs')")

actual fun removeDirectory(directoryPath: String) {
    fs.rmSync(directoryPath, json("recursive" to true, "force" to true))
}
