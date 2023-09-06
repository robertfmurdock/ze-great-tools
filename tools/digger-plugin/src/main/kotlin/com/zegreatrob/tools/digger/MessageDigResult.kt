package com.zegreatrob.tools.digger

data class MessageDigResult(
    val storyId: String?,
    val ease: Int?,
    val coauthors: List<String>,
)

data class CommitInspectionResult(
    val storyId: String?,
    val ease: Int?,
    val authors: List<String>,
)
