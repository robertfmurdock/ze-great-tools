package com.zegreatrob.tools.digger.guide

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external object NodeFs {
    fun readFileSync(path: String, options: dynamic): String
}

@JsModule("node:path")
@JsNonModule
private external object NodePath {
    fun join(vararg paths: String): String
}

@JsName("__dirname")
private external val nodeDirname: String

actual fun getDiggerGuideContent(): String =
    NodeFs.readFileSync(NodePath.join(nodeDirname, "help", "digger-guide.md"), json("encoding" to "utf-8"))
