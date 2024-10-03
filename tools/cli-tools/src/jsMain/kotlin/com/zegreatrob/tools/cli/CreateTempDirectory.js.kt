package com.zegreatrob.tools.cli

private val fs = js("require('node:fs')")
private val path = js("require('node:path')")
private val os = js("require('node:os')")

actual fun createTempDirectory(): String = fs.mkdtempSync(path.join(os.tmpdir(), "/zgt")).unsafeCast<String>()
