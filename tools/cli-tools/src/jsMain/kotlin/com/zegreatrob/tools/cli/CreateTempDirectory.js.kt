package com.zegreatrob.tools.cli

private val fs = js("require('node:fs')")

actual fun createTempDirectory(): String = fs.mkdtempSync("zgt").unsafeCast<String>()
