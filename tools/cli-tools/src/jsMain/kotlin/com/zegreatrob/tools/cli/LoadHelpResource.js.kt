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
    val candidatePaths = listOf(
        nodePath.join(dirname, path),
        nodePath.join(dirname, "../..", "processedResources/js/main", path),
        nodePath.join(dirname, "../../..", "processedResources/js/main", path),
        nodePath.join(dirname, "../../../..", "processedResources/js/main", path),
    )

    for (candidate in candidatePaths) {
        if (fs.existsSync(candidate).unsafeCast<Boolean>()) {
            return fs.readFileSync(candidate, json("encoding" to "utf-8")).unsafeCast<String>()
        }
    }

    throw IllegalStateException("Could not find resource: $path. Tried: ${candidatePaths.joinToString()}")
}
