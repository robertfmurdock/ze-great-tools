package com.zegreatrob.tools.tagger.core

private fun String.isSnapshot() = contains("SNAPSHOT")

fun TaggerCore.tag(
    version: String,
    releaseBranch: String?,
    userName: String?,
    userEmail: String?,
    allowDetachedHead: Boolean = false,
): TagResult {
    val isSnapshot = version.isSnapshot()
    val headTag = adapter.showTag("HEAD")
    val alreadyTagged = headTag != null
    val headBranch = adapter.status().head
    val isNotOnReleaseBranch = !allowDetachedHead && headBranch != releaseBranch
    return if (isSnapshot || alreadyTagged || isNotOnReleaseBranch) {
        TagResult.Warning(
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
            }.getOrElse { error ->
                TagResult.Warning(
                    when (error) {
                        is com.zegreatrob.tools.adapter.git.ProcessError -> error.toUserMessage()
                        else -> error.message ?: "Unknown error during tagging"
                    },
                )
            }
    }
}
