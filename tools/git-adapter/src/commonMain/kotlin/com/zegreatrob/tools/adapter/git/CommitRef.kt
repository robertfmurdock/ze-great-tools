package com.zegreatrob.tools.adapter.git

import kotlin.time.Instant

data class CommitRef(
    val id: String,
    val authorEmail: String,
    val committerEmail: String,
    val dateTime: Instant,
    val parents: List<String>,
    val fullMessage: String,
)
