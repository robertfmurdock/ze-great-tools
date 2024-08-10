package com.zegreatrob.tools.digger.core

class DiggerCore(
    private val label: String?,
    private val gitWrapper: DiggerGitWrapper,
    private val messageDigger: MessageDigger,
) {
    fun currentContributionData() =
        with(gitWrapper) {
            val (currentTag, previousTag) = currentRelevantTags(
                headCommitId = headCommitId(),
                lastTwoTags = listTags().take(2),
            )
            messageDigger.contribution(currentContributionCommits(previousTag))
                .copy(
                    label = label,
                    tagName = currentTag?.name,
                    tagDateTime = currentTag?.dateTime,
                )
        }

    private fun currentRelevantTags(
        headCommitId: String,
        lastTwoTags: List<TagRef>,
    ) = lastTwoTags.getOrNull(0).let { latestTag ->
        if (latestTag?.commitId == headCommitId) {
            latestTag to lastTwoTags.getOrNull(1)
        } else {
            null to latestTag
        }
    }

    fun allContributionData() = gitWrapper
        .allContributionCommits()
        .map { range -> range.first to messageDigger.contribution(range.second.toList()) }
        .map { (tag, contribution) ->
            contribution.copy(
                label = label,
                tagName = tag?.name,
                tagDateTime = tag?.dateTime,
            )
        }
}
