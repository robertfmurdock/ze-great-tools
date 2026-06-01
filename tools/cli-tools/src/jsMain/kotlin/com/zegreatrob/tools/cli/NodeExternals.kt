package com.zegreatrob.tools.cli

@JsModule("node:fs")
@JsNonModule
internal external object NodeFs {
    fun readFileSync(path: String, options: dynamic): String
    fun writeFileSync(path: String, data: String)
    fun existsSync(path: String): Boolean
}

@JsModule("node:path")
@JsNonModule
internal external object NodePath {
    fun join(vararg paths: String): String
}

@JsName("__dirname")
internal external val nodeDirname: String
