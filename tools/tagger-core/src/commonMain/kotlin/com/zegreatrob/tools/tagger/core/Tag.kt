package com.zegreatrob.tools.tagger.core

private fun String.isSnapshot() = contains("SNAPSHOT")

fun TaggerCore.tag(
    version: String,
    releaseBranch: String?,
    userName: String?,
    userEmail: String?,
    allowDetachedHead: Boolean = false,
): TagResult {
    val tagState = checkTagExistence(version)
    if (tagState.existsOnSameCommit) return TagResult.Success

    val branchState = checkBranchState(releaseBranch, allowDetachedHead)
    val validationErrors = buildValidationErrors(version, releaseBranch, tagState, branchState)

    return if (validationErrors.isNotEmpty()) {
        TagResult.Warning(TagErrors.wrapper(validationErrors.joinToString(", ")))
    } else {
        createAndPushTag(version, userName, userEmail)
    }
}

private data class TagState(
    val existsOnSameCommit: Boolean,
    val existsOnDifferentCommit: Boolean,
    val headTag: com.zegreatrob.tools.adapter.git.TagRef?,
)

private fun TaggerCore.checkTagExistence(version: String): TagState {
    val headTag = adapter.showTag("HEAD")
    val headCommit = adapter.headCommitId()
    val existingTag = adapter.listTags().find { it.name == version }
    return TagState(
        existsOnSameCommit = existingTag != null && existingTag.commitId == headCommit,
        existsOnDifferentCommit = existingTag != null && existingTag.commitId != headCommit,
        headTag = headTag,
    )
}

private data class BranchState(
    val headBranch: String,
    val isNotOnReleaseBranch: Boolean,
)

private fun TaggerCore.checkBranchState(releaseBranch: String?, allowDetachedHead: Boolean): BranchState {
    val headBranch = adapter.status().head
    val isDetachedHead = headBranch == "HEAD" || headBranch.startsWith("(detached")
    return BranchState(
        headBranch = headBranch,
        isNotOnReleaseBranch = headBranch != releaseBranch && !(allowDetachedHead && isDetachedHead),
    )
}

private fun buildValidationErrors(
    version: String,
    releaseBranch: String?,
    tagState: TagState,
    branchState: BranchState,
): List<String> {
    val isSnapshot = version.isSnapshot()
    val alreadyTagged = tagState.headTag != null && tagState.headTag.name != version
    return mapOf(
        isSnapshot to TagErrors.BEING_SNAPSHOT,
        alreadyTagged to TagErrors.alreadyTagged(tagState.headTag?.name),
        branchState.isNotOnReleaseBranch to TagErrors.skipMessageNotOnReleaseBranch(releaseBranch, branchState.headBranch),
        tagState.existsOnDifferentCommit to "Tag $version already exists on a different commit",
    ).filterKeys { it }.values.toList()
}

private fun TaggerCore.createAndPushTag(
    version: String,
    userName: String?,
    userEmail: String?,
): TagResult = kotlin.runCatching { adapter.newAnnotatedTag(version, "HEAD", userName, userEmail) }
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
