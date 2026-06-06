package com.zegreatrob.tools.fingerprint

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun java.io.File.writeToFile(content: String) {
    parentFile?.mkdirs()
    writeText(content)
}
