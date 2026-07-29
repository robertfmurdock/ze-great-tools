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
    val headCommit = adapter.headCommitId()

    val existingTag = adapter.listTags().find { it.name == version }
    val tagExistsOnDifferentCommit = existingTag != null && existingTag.commitId != headCommit
    val tagExistsOnSameCommit = existingTag != null && existingTag.commitId == headCommit

    val alreadyTagged = headTag != null && headTag.name != version
    val headBranch = adapter.status().head
    val isDetachedHead = headBranch == "HEAD" || headBranch.startsWith("(detached")
    val isNotOnReleaseBranch = headBranch != releaseBranch && !(allowDetachedHead && isDetachedHead)

    return if (tagExistsOnSameCommit) {
        TagResult.Success
    } else if (isSnapshot || alreadyTagged || isNotOnReleaseBranch || tagExistsOnDifferentCommit) {
        TagResult.Warning(
            TagErrors.wrapper(
                mapOf(
                    isSnapshot to TagErrors.BEING_SNAPSHOT,
                    alreadyTagged to TagErrors.alreadyTagged(headTag?.name),
                    isNotOnReleaseBranch to TagErrors.skipMessageNotOnReleaseBranch(releaseBranch, headBranch),
                    tagExistsOnDifferentCommit to "Tag $version already exists on a different commit",
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
