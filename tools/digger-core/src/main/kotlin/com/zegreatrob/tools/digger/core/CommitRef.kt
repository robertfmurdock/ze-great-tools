package com.zegreatrob.tools.digger.core

import kotlinx.datetime.Instant

data class CommitRef(
    val id: String,
    val authorEmail: String,
    val committerEmail: String,
    val dateTime: Instant,
    val fullMessage: String,
)
