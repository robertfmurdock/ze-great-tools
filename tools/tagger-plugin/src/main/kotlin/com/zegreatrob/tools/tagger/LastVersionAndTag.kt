package com.zegreatrob.tools.tagger

import org.ajoberstar.grgit.Grgit

fun Grgit.lastVersionAndTag(): Pair<String, String>? {
    val description: String? = describe { abbrev = 0 }
    if (description == null) {
        return null
    }
    val previousVersionNumber =
        if (description.contains("-")) {
            description.substringBefore("-")
        } else {
            description
        }
    return previousVersionNumber.ifEmpty { null }
        ?.let { it to description }
}
