package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.currentCommitTag(): TagRef? {
    val firstTag = listTags().maxByOrNull { it.dateTime }
    val headCommitId = headCommitId()
    return if (firstTag?.commitId != headCommitId) {
        null
    } else {
        firstTag
    }
}
