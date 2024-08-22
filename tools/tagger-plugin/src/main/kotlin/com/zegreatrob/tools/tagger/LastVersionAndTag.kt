package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter

fun lastVersionAndTag(adapter: GitAdapter): Pair<String, String>? {
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
