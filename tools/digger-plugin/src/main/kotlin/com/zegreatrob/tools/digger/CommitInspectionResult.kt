package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.SemverType

data class CommitInspectionResult(
    val storyId: String?,
    val ease: Int?,
    val authors: List<String>,
    val semver: SemverType?,
)
