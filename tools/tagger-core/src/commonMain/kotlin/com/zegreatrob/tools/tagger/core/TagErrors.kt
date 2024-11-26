package com.zegreatrob.tools.tagger.core

object TagErrors {
    fun wrapper(
        messages: String,
    ) = "skipping tag due to $messages"

    const val BEING_SNAPSHOT = "being snapshot"
    fun alreadyTagged(headTag: String?) = "already tagged $headTag"
    fun skipMessageNotOnReleaseBranch(releaseBranch: String?, headBranch: String) = "not on release branch $releaseBranch - branch was $headBranch"
}
