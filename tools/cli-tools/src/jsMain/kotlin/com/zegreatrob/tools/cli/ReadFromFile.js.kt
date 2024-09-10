package com.zegreatrob.tools.cli

import kotlin.js.json

private val fs = js("require('node:fs')")
actual fun readFromFile(fileName: String): String? =
    fs.readFileSync(fileName, json("encoding" to "utf-8"))
        .unsafeCast<String>()
