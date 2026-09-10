package com.zegreatrob.tools.adapter.git

external interface NodeChildProcess {
    fun spawnSync(command: String, args: Array<String>, options: SpawnSyncOptions): SpawnSyncReturns
}

external interface SpawnSyncOptions {
    var cwd: String?
    var maxBuffer: Number?
    var env: dynamic
    var shell: Boolean?
    var windowsHide: Boolean?
    var encoding: String?
}

external interface SpawnSyncReturns {
    val pid: Int
    val output: Array<dynamic>
    val stdout: dynamic
    val stderr: dynamic
    val status: Int?
    val signal: String?
    val error: Throwable?
}

external interface NodeProcess {
    val env: dynamic
}

@JsModule("node:child_process")
@JsNonModule
external val childProcess: NodeChildProcess

@JsModule("node:process")
@JsNonModule
external val process: NodeProcess

external class Object {
    companion object {
        fun assign(target: dynamic, vararg sources: dynamic): dynamic
    }
}

inline fun <T : Any> jsObject(builder: T.() -> Unit): T = (js("({})").unsafeCast<T>()).apply(builder)
