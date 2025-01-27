package com.zegreatrob.tools.test.git

import kotlin.js.json

@JsModule("node:fs")
@JsNonModule
private external val fs: dynamic

actual fun removeDirectory(directoryPath: String) {
    fs.rmSync(directoryPath, json("recursive" to true, "force" to true))
}
