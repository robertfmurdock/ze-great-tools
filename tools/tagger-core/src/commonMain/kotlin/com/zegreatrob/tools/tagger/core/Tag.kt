package com.zegreatrob.tools.tagger.core

private fun String.isSnapshot() = contains("SNAPSHOT")

fun TaggerCore.tag(version: String, releaseBranch: String?): TagResult {
    val isSnapshot = version.isSnapshot()
    val headTag = adapter.showTag("HEAD")
    val alreadyTagged = headTag != null
    val headBranch = adapter.status().head
    val isNotOnReleaseBranch = headBranch != releaseBranch
    return if (isSnapshot || alreadyTagged || isNotOnReleaseBranch) {
        TagResult.Error(
            "skipping tag due to ${
                mapOf(
                    isSnapshot to "being snapshot",
                    alreadyTagged to "already tagged $headTag",
                    isNotOnReleaseBranch to "not on release branch $releaseBranch - branch was $headBranch",
                )
                    .filterKeys { it }
                    .values.joinToString(", ")
            }",
        )
    } else {
        adapter.newAnnotatedTag(version, "HEAD")
        adapter.pushTags()
        TagResult.Success
    }
}
