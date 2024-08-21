package com.zegreatrob.tools.wrapper.git

import kotlin.js.json

private val childProcess = js("require('node:child_process')")

actual fun runProcess(args: List<String>, workingDirectory: String): String {
    val program = args.first()
    val spawn = childProcess.spawnSync(
        program,
        args.subList(1, args.size).toTypedArray(),
        json("cwd" to workingDirectory, "maxBuffer" to 1024 * 1024 * 10),
    )
    return spawn.stdout?.toString("utf8").unsafeCast<String>()
}
