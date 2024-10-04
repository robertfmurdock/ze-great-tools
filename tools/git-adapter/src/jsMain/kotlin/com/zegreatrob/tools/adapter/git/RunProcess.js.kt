package com.zegreatrob.tools.adapter.git

import kotlin.js.Json
import kotlin.js.json

private val childProcess = js("require('node:child_process')")

actual fun runProcess(args: List<String>, workingDirectory: String, env: Map<String, String>): String {
    val program = args.first()
    val spawn = childProcess.spawnSync(
        program,
        args.subList(1, args.size).toTypedArray(),
        json(
            "cwd" to workingDirectory,
            "maxBuffer" to 1024 * 1024 * 10,
            "env" to env.toJson(),
        ),
    )
    if (spawn.status != 0) {
        throw Exception(spawn.stderr?.toString("utf8").unsafeCast<String>())
    }

    return spawn.stdout?.toString("utf8").unsafeCast<String>()
}

private fun Map<String, String>.toJson(): Json = json(pairs = toList().toTypedArray())
