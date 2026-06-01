package com.zegreatrob.tools.cli

actual fun loadHelpResource(path: String): String = object {}.javaClass.classLoader
    ?.getResourceAsStream(path)
    ?.bufferedReader()
    ?.readText()
    ?: error("Failed to load help resource: $path")
