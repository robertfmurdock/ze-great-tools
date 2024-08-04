package com.zegreatrob.tools.digger.core

import kotlin.js.json

private val childProcess = js("require('node:child_process')")

actual fun runProcess(args: List<String>, workingDirectory: String): String {
    val program = args.first()
    val spawn = childProcess.spawnSync(
        program,
        args.subList(1, args.size).toTypedArray(),
        json("cwd" to workingDirectory),
    )
    return spawn.stdout?.toString("utf8").unsafeCast<String>()
}
