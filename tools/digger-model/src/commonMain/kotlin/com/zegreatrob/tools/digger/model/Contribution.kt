package com.zegreatrob.tools.digger.model

import kotlinx.datetime.Instant

data class Contribution(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: Instant? = null,
    val firstCommitDateTime: Instant? = null,
    val ease: Int? = null,
    val storyId: String? = null,
    val semver: String? = null,
    val label: String? = null,
    val tagName: String? = null,
    val tagDateTime: Instant? = null,
)
