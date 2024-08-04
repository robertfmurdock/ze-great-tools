package com.zegreatrob.tools.digger.core

class DiggerCore(
    private val label: String?,
    private val gitWrapper: DiggerGitWrapper,
    private val messageDigger: MessageDigger,
) {
    fun currentContributionData() =
        with(gitWrapper) {
            val currentCommitTag = currentCommitTag()
            messageDigger.contribution(currentContributionCommits())
                .copy(
                    label = label,
                    tagName = currentCommitTag?.name,
                    tagDateTime = currentCommitTag?.dateTime,
                )
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
