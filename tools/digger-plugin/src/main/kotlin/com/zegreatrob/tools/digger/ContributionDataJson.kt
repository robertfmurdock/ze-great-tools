package com.zegreatrob.tools.digger

data class ContributionDataJson(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
)
