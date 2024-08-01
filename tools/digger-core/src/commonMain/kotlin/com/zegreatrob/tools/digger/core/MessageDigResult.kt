package com.zegreatrob.tools.digger.core

data class MessageDigResult(
    val storyId: String?,
    val ease: Int?,
    val coauthors: List<String>,
    val semver: SemverType?,
)
