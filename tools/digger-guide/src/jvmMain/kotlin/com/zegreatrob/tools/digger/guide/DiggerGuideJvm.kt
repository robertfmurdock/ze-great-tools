package com.zegreatrob.tools.digger.guide

actual fun getDiggerGuideContent(): String = object {}.javaClass.classLoader.getResourceAsStream("help/digger-guide.md")
    ?.bufferedReader()
    ?.use { it.readText() }
    ?: error("digger-guide.md not found in resources")
