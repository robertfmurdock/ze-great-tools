package com.zegreatrob.tools.cli

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external val fs: dynamic

@JsModule("node:path")
@JsNonModule
private external val nodePath: dynamic

@JsName("__dirname")
private external val dirname: String

actual fun loadHelpResource(path: String): String {
    val resourcePath = nodePath.join(dirname, path)
    return fs.readFileSync(resourcePath, json("encoding" to "utf-8")).unsafeCast<String>()
}
