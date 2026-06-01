package com.zegreatrob.tools.cli

import kotlin.js.json

actual fun readFromFile(fileName: String): String? = if (NodeFs.existsSync(fileName)) {
    NodeFs.readFileSync(fileName, json("encoding" to "utf-8"))
} else {
    null
}
