package com.zegreatrob.tools.test.git

@JsModule("node:fs")
@JsNonModule
private external val fs: dynamic

@JsModule("node:path")
@JsNonModule
private external val path: dynamic

@JsModule("node:os")
@JsNonModule
private external val os: dynamic

actual fun createTempDirectory(): String = fs.mkdtempSync(path.join(os.tmpdir(), "/zgt")).unsafeCast<String>()
