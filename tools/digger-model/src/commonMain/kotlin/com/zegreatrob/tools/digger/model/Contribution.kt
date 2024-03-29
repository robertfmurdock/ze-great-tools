package com.zegreatrob.tools.digger.model

import kotlinx.datetime.Instant

data class Contribution(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: Instant?,
    val ease: Int?,
    val storyId: String?,
    val semver: String?,
    val label: String?,
)
