package com.zegreatrob.tools.tagger.core

data class VersionRegex(
    val none: Regex,
    val patch: Regex,
    val minor: Regex,
    val major: Regex,
    val unified: Regex?,
)
