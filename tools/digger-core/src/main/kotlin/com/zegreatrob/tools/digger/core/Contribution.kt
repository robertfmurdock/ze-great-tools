package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit

fun List<Commit>.contribution(): Contribution {
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
        dateTime = lastCommit?.dateTime?.toInstant()?.toKotlinInstant(),
        firstCommitDateTime = firstCommit?.dateTime?.toInstant()?.toKotlinInstant(),
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

private fun Commit.commitInspectionResult(digResult: MessageDigResult) =
    CommitInspectionResult(
        storyId = digResult.storyId,
        ease = digResult.ease,
        authors = listOf(committer.email, author.email) + digResult.coauthors,
        semver = digResult.semver,
    )
