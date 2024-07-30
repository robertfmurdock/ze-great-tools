package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.model.Contribution

fun List<CommitRef>.contribution(): Contribution {
    val messageDigResults =
        map { commit ->
            commit.commitInspectionResult(MessageDigger().digIntoMessage(commit.fullMessage))
        }

    val lastCommit = firstOrNull()
    val firstCommit = lastOrNull()
    return Contribution(
        lastCommit = lastCommit?.id ?: "",
        firstCommit = firstCommit?.id ?: "",
        authors = allAuthors(messageDigResults),
        dateTime = lastCommit?.dateTime,
        firstCommitDateTime = firstCommit?.dateTime,
        ease = messageDigResults.mapNotNull { it.ease }.maxOrNull(),
        storyId = mergedStoryIds(messageDigResults),
        semver = messageDigResults.mapNotNull { it.semver }.highestPrioritySemver()?.toString(),
        label = null,
        tagName = null,
        tagDateTime = null,
        commitCount = count(),
    )
}

private fun allAuthors(messageDigResults: List<CommitInspectionResult>) = messageDigResults
    .flatMap(CommitInspectionResult::authors)
    .map { it.lowercase() }
    .toSet()
    .sorted()
    .toList()

private fun mergedStoryIds(messageDigResults: List<CommitInspectionResult>) = messageDigResults
    .mapNotNull(CommitInspectionResult::storyId)
    .let {
        if (it.isNotEmpty()) {
            it.toSortedSet().joinToString(", ")
        } else {
            null
        }
    }

private fun CommitRef.commitInspectionResult(digResult: MessageDigResult) =
    CommitInspectionResult(
        storyId = digResult.storyId,
        ease = digResult.ease,
        authors = listOf(committerEmail, authorEmail) + digResult.coauthors,
        semver = digResult.semver,
    )
