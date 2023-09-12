package com.zegreatrob.tools.digger.model

data class Contribution(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: String?,
    val ease: Int?,
    val storyId: String?,
    val semver: String?,
)
