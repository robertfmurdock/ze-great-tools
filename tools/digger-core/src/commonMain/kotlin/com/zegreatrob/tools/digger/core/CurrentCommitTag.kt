package com.zegreatrob.tools.digger.core

fun DiggerGitWrapper.currentCommitTag(): TagRef? = findRelatedTag(headCommitId())

private fun DiggerGitWrapper.findRelatedTag(headCommitId: String): TagRef? = listTags()
    .find { it.commitId == headCommitId }
