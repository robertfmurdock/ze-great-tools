package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.model.Contribution

fun MessageDigger.contribution(commitRefs: List<CommitRef>): Contribution {
    val messageDigResults =
        commitRefs.map { commit ->
            commit.commitInspectionResult(digIntoMessage(commit.fullMessage))
        }

    val lastCommit = commitRefs.firstOrNull()
    val firstCommit = commitRefs.lastOrNull()
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
        commitCount = commitRefs.count(),
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
            it.toSet().sorted().joinToString(", ")
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
