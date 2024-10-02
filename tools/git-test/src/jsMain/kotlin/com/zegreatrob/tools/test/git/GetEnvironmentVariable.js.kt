package com.zegreatrob.tools.test.git

actual fun getEnvironmentVariable(name: String): String? = js("process.env")[name].unsafeCast<String?>()
