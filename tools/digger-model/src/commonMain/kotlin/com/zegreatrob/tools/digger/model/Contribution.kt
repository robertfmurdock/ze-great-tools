@file:OptIn(ExperimentalTime::class)

package com.zegreatrob.tools.digger.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Contribution(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val commitCount: Int,
    val dateTime: Instant?,
    val firstCommitDateTime: Instant?,
    val ease: Int?,
    val storyId: String?,
    val semver: String?,
    val label: String?,
    val tagName: String?,
    val tagDateTime: Instant?,
)
