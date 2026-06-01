package com.zegreatrob.tools.cli

import kotlin.js.json

actual fun loadHelpResource(path: String): String {
    val resourcePath = NodePath.join(nodeDirname, path)
    return NodeFs.readFileSync(resourcePath, json("encoding" to "utf-8"))
}
