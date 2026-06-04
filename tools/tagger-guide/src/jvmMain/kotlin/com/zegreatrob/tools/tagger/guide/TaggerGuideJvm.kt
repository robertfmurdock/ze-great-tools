package com.zegreatrob.tools.tagger.guide

actual fun getTaggerGuideContent(): String = object {}.javaClass.classLoader.getResourceAsStream("help/tagger-guide.md")
    ?.bufferedReader()
    ?.use { it.readText() }
    ?: error("tagger-guide.md not found in resources")
