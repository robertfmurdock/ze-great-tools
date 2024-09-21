package com.zegreatrob.tools.adapter.git

data class GitStatus(
    val isClean: Boolean,
    val ahead: Int,
    val behind: Int,
    val head: String,
    val upstream: String,
)
