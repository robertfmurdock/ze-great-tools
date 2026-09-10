package com.zegreatrob.tools.adapter.git

import java.io.File
import java.nio.charset.Charset

actual fun runProcess(args: List<String>, workingDirectory: String, env: Map<String, String>): String {
    require(args.isNotEmpty()) { "Cannot run process with empty arguments list" }
    val program = args.first()
    require(program.isNotBlank()) { "Executable program name cannot be blank" }
    val process = ProcessBuilder(args)
        .also { it.environment().putAll(env) }
        .directory(File(workingDirectory))
        .start()
    val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
    val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
    process.waitFor()
    if (process.exitValue() != 0) {
        throw ProcessError(
            exitCode = process.exitValue(),
            stderr = error,
            command = args.joinToString(" "),
        )
    }
    return outputText
}
