package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit

fun List<Commit>.contribution(): Contribution {
    val messageDigResults = map { commit ->
        commit.commitInspectionResult(MessageDigger().digIntoMessage(commit.fullMessage))
    }

    return Contribution(
        lastCommit = firstOrNull()?.id ?: "",
        firstCommit = lastOrNull()?.id ?: "",
        authors = messageDigResults.flatMap { it.authors }
            .map { it.lowercase() }
            .toSet()
            .sorted()
            .toList(),
        dateTime = firstOrNull()?.dateTime?.toInstant()?.toKotlinInstant(),
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
        label = null,
    )
}

private fun Commit.commitInspectionResult(digResult: MessageDigResult) = CommitInspectionResult(
    storyId = digResult.storyId,
    ease = digResult.ease,
    authors = listOf(committer.email, author.email) + digResult.coauthors,
    semver = digResult.semver,
)
