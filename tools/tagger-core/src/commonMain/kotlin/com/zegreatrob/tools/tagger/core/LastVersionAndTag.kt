package com.zegreatrob.tools.tagger.core

fun TaggerCore.lastVersionAndTag(): Pair<String, String>? {
    val description: String = adapter.describe(abbrev = 0)
        ?: return null
    val previousVersionNumber =
        if (description.contains("-")) {
            description.substringBefore("-")
        } else {
            description
        }
    return previousVersionNumber.ifEmpty { null }
        ?.let { it to description }
}
