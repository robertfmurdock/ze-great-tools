package com.zegreatrob.tools.digger.core

import org.ajoberstar.grgit.Commit

data class ContributionDataJson(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: String?,
    val ease: Int?,
    val storyId: String?,
    val semver: String?,
)

fun List<Commit>.contributionDataJson(): ContributionDataJson {
    val messageDigResults = map { commit ->
        commit.commitInspectionResult(MessageDigger().digIntoMessage(commit.fullMessage))
    }

    return ContributionDataJson(
        firstCommit = lastOrNull()?.id ?: "",
        lastCommit = firstOrNull()?.id ?: "",
        dateTime = firstOrNull()?.dateTime?.toString(),
        authors = messageDigResults.flatMap { it.authors }
            .map { it.lowercase() }
            .toSet()
            .sorted()
            .toList(),
        ease = messageDigResults.mapNotNull { it.ease }.maxOrNull(),
        storyId = messageDigResults.mapNotNull { it.storyId }
            .let {
                if (it.isEmpty()) {
                    null
                } else {
                    it.toSortedSet().joinToString(", ")
                }
            },
        semver = messageDigResults.mapNotNull { it.semver }.highestPrioritySemver()?.toString(),
    )
}

private fun Commit.commitInspectionResult(digResult: MessageDigResult) = CommitInspectionResult(
    storyId = digResult.storyId,
    ease = digResult.ease,
    authors = listOf(committer.email, author.email) + digResult.coauthors,
    semver = digResult.semver,
)
