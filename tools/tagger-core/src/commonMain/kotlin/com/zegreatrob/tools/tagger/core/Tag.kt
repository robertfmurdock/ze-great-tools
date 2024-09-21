package com.zegreatrob.tools.tagger.core

private fun String.isSnapshot() = contains("SNAPSHOT")

fun TaggerCore.tag(version: String, releaseBranch: String?, userName: String?, userEmail: String?): TagResult {
    val isSnapshot = version.isSnapshot()
    val headTag = adapter.showTag("HEAD")
    val alreadyTagged = headTag != null
    val headBranch = adapter.status().head
    val isNotOnReleaseBranch = headBranch != releaseBranch
    return if (isSnapshot || alreadyTagged || isNotOnReleaseBranch) {
        TagResult.Error(
            TagErrors.wrapper(
                mapOf(
                    isSnapshot to TagErrors.BEING_SNAPSHOT,
                    alreadyTagged to TagErrors.alreadyTagged(headTag?.name),
                    isNotOnReleaseBranch to TagErrors.skipMessageNotOnReleaseBranch(releaseBranch, headBranch),
                ).filterKeys { it }.values.joinToString(", "),
            ),
        )
    } else {
        kotlin.runCatching { adapter.newAnnotatedTag(version, "HEAD", userName, userEmail) }
            .map {
                adapter.pushTags()
                TagResult.Success
            }.getOrElse {
                TagResult.Error(it.message ?: "Unknown error during tagging")
            }
    }
}
