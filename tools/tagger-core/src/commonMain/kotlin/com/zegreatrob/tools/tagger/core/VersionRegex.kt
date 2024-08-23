package com.zegreatrob.tools.tagger.core

data class VersionRegex(
    val none: Regex,
    val patch: Regex,
    val minor: Regex,
    val major: Regex,
    val unified: Regex?,
) {
    object Defaults {
        val none = Regex("\\[none].*", RegexOption.IGNORE_CASE)
        val patch = Regex("\\[patch].*", RegexOption.IGNORE_CASE)
        val minor = Regex("\\[minor].*", RegexOption.IGNORE_CASE)
        val major = Regex("\\[major].*", RegexOption.IGNORE_CASE)
    }
}
