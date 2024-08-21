package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.wrapper.git.GitAdapter
import com.zegreatrob.tools.wrapper.git.TagRef

class DiggerCore(
    private val label: String?,
    private val gitWrapper: GitAdapter,
    private val messageDigger: MessageDigger,
    private val tagRegex: Regex = Defaults.tagRegex,
) {
    private fun tagRefs() = gitWrapper.listTags().filter { tagRegex.matches(it.name) }

    fun currentContributionData() =
        with(gitWrapper) {
            val (currentTag, previousTag) = currentRelevantTags(
                headCommitId = headCommitId(),
                lastTwoTags = tagRefs().take(2),
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
        .allContributionCommits(tagRefs(), gitWrapper.log())
        .map { range -> range.first to messageDigger.contribution(range.second.toList()) }
        .map { (tag, contribution) ->
            contribution.copy(
                label = label,
                tagName = tag?.name,
                tagDateTime = tag?.dateTime,
            )
        }

    object Defaults {
        val tagRegex: Regex = Regex(".*")
    }
}
