package com.zegreatrob.tools.cli

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external val fs: dynamic

actual fun readFromFile(fileName: String): String? = if (fs.existsSync(fileName).unsafeCast<Boolean>()) {
    fs.readFileSync(fileName, json("encoding" to "utf-8"))
        .unsafeCast<String>()
} else {
    null
}
