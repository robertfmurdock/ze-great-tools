package com.zegreatrob.tools.wrapper.git

import java.io.File
import java.nio.charset.Charset

actual fun runProcess(args: List<String>, workingDirectory: String): String {
    val process = ProcessBuilder(args)
        .directory(File(workingDirectory))
        .start()
    val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
    val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
    process.waitFor()
    if (error.isNotEmpty()) {
        throw Error(error)
    }
    return outputText
}
