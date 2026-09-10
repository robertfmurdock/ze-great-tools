package com.zegreatrob.tools.adapter.git

import kotlin.js.Json
import kotlin.js.json

actual fun runProcess(args: List<String>, workingDirectory: String, env: Map<String, String>): String {
    require(args.isNotEmpty()) { "Cannot run process with empty arguments list" }
    val program = args.first()
    require(program.isNotBlank()) { "Executable program name cannot be blank" }

    val mergedEnv = jsObject<dynamic> {
        Object.assign(this, process.env, env.toJson())
    }

    val options = jsObject<SpawnSyncOptions> {
        cwd = workingDirectory
        maxBuffer = 1024 * 1024 * 10
        this.env = mergedEnv
        shell = false
        windowsHide = true
    }

    val spawn = childProcess.spawnSync(
        command = program,
        args = args.subList(1, args.size).toTypedArray(),
        options = options,
    )

    if (spawn.status != 0) {
        val stderr = spawn.stderr?.toString("utf8").unsafeCast<String?>() ?: ""
        val exitCode = spawn.status ?: -1
        throw ProcessError(
            exitCode = exitCode,
            stderr = stderr,
            command = args.joinToString(" "),
        )
    }

    return spawn.stdout?.toString("utf8").unsafeCast<String?>() ?: ""
}

private fun Map<String, String>.toJson(): Json = json(pairs = toList().toTypedArray())
