package com.zegreatrob.tools.digger.cli

private val process = js("require('node:process')")

actual fun platformArgCorrection(args: Array<String>): Array<String> {
    val argv = process.argv.unsafeCast<Array<String>>()
    return argv.slice(2..<argv.size).toTypedArray()
}
