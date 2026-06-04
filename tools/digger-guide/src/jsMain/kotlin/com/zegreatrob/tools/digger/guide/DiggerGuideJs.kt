package com.zegreatrob.tools.digger.guide

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external object NodeFs {
    fun readFileSync(path: String, options: dynamic): String
}

actual fun getDiggerGuideContent(): String =
    NodeFs.readFileSync("./kotlin/help/digger-guide.md", json("encoding" to "utf-8"))
