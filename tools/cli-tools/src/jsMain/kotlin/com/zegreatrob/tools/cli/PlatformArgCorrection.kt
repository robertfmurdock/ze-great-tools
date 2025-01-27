package com.zegreatrob.tools.cli

@JsModule("node:process")
@JsNonModule
private external val process: dynamic

actual fun platformArgCorrection(args: Array<String>): Array<String> {
    val argv = process.argv.unsafeCast<Array<String>>()
    return argv.slice(2..<argv.size).toTypedArray()
}
