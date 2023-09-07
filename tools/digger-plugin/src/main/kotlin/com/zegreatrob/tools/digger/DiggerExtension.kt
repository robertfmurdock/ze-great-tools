package com.zegreatrob.tools.digger

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.operation.LogOp

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun allContributionData() = grgitServiceExtension.service.get().grgit
        .allContributionCommits()
        .map { range -> range.toList().contributionDataJson() }

    private fun List<Commit>.contributionDataJson(): ContributionDataJson {
        val messageDigResults = map { commit ->
            commit.commitInspectionResult(digIntoMessage(commit.fullMessage))
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
            semver = messageDigResults.firstNotNullOfOrNull { it.semver },
        )
    }

    private fun Commit.commitInspectionResult(
        it: MessageDigResult,
    ) = CommitInspectionResult(
        storyId = it.storyId,
        ease = it.ease,
        authors = listOf(committer.email, author.email) + it.coauthors,
        semver = it.semver?.toString(),
    )

    fun currentContributionData() = grgitServiceExtension.service.get().grgit
        .currentContributionCommits()
        .contributionDataJson()

    private fun Grgit.currentContributionCommits(): List<Commit> {
        val tag = previousTag()
        return if (tag == null) {
            log()
        } else {
            return log(fun(it: LogOp) { it.range(tag, "HEAD") })
        }
    }

    private fun Grgit.allContributionCommits(): List<List<Commit>> {
        val tagList = tag.list()
        return log().fold(emptyList()) { acc, commit ->
            if (tagList.any { it.commit == commit }) {
                acc.plus(element = listOf(commit))
            } else {
                val lastList = acc.lastOrNull()
                if (lastList != null) {
                    acc.slice(0..acc.size - 2).plus(element = lastList.plusElement(commit))
                } else {
                    acc.plus(element = listOf(commit))
                }
            }
        }
    }

    private fun Grgit.previousTag(): Tag? {
        val tagList = tag.list().sortedByDescending { it.dateTime }
        val tag = tagList.firstOrNull()
        return if (tag?.commit == head()) {
            tagList.getOrNull(1)
        } else {
            tag
        }
    }

    fun headId(): String = grgitServiceExtension.service.get().grgit.head().id
}
