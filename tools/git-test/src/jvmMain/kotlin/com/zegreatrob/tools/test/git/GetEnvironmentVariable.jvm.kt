package com.zegreatrob.tools.test.git

actual fun getEnvironmentVariable(name: String): String? = System.getenv(name)
