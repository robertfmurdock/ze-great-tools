package com.zegreatrob.tools.cli

import kotlin.js.json

private val fs = js("require('node:fs')")
actual fun readFromFile(fileName: String): String? = if (fs.existsSync(fileName).unsafeCast<Boolean>()) {
    fs.readFileSync(fileName, json("encoding" to "utf-8"))
        .unsafeCast<String>()
} else {
    null
}
