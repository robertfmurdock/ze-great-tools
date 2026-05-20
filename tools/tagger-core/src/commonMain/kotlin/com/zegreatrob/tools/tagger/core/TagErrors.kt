package com.zegreatrob.tools.tagger.core

object TagErrors {
    fun wrapper(
        messages: String,
    ) = "skipping tag due to $messages"

    const val BEING_SNAPSHOT = "being snapshot. Run 'tagger calculate-version' to see why this version has -SNAPSHOT suffix"
    fun alreadyTagged(headTag: String?) = "already tagged $headTag"
    fun skipMessageNotOnReleaseBranch(releaseBranch: String?, headBranch: String) = "not on release branch $releaseBranch - branch was $headBranch"
}
