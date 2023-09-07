package com.zegreatrob.tools.digger.core

data class CommitInspectionResult(
    val storyId: String?,
    val ease: Int?,
    val authors: List<String>,
    val semver: SemverType?,
)
