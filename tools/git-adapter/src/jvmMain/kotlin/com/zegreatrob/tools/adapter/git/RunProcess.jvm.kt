package com.zegreatrob.tools.adapter.git

import java.io.File
import java.nio.charset.Charset

actual fun runProcess(args: List<String>, workingDirectory: String, env: Map<String, String>): String {
    val process = ProcessBuilder(args)
        .also { it.environment().putAll(env) }
        .directory(File(workingDirectory))
        .start()
    val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
    val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
    process.waitFor()
    if (process.exitValue() != 0) {
        throw Error("$outputText\n$error")
    }
    return outputText
}
