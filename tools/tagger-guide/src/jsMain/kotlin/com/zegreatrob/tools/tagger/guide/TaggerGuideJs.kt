package com.zegreatrob.tools.tagger.guide

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external object NodeFs {
    fun readFileSync(path: String, options: dynamic): String
}

actual fun getTaggerGuideContent(): String =
    NodeFs.readFileSync("./kotlin/help/tagger-guide.md", json("encoding" to "utf-8"))
