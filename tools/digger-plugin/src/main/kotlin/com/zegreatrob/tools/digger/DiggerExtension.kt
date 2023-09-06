package com.zegreatrob.tools.digger

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.operation.LogOp

val coAuthorRegex = Regex("Co-authored-by: (.*) <(.*)>", RegexOption.IGNORE_CASE)
val easeRegex = Regex(".*-(?<ease>[1-5])-.*", RegexOption.IGNORE_CASE)
val storyRegex = Regex("\\[(?<storyId>.*)].*", RegexOption.IGNORE_CASE)

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun allContributionData() = grgitServiceExtension.service.get().grgit
        .allContributionCommits()
        .map { range -> range.toList().contributionDataJson() }

    private fun List<Commit>.contributionDataJson() = ContributionDataJson(
        firstCommit = lastOrNull()?.id ?: "",
        lastCommit = firstOrNull()?.id ?: "",
        dateTime = lastOrNull()?.dateTime?.toString(),
        authors = flatMap { commit -> commit.allAuthors() }.toSet()
            .sortedBy(CoAuthor::email)
            .map(CoAuthor::email)
            .toList(),
        ease = mapNotNull { commit -> commit.ease() }.maxOrNull(),
        storyId = mapNotNull { commit -> commit.storyId() }
            .let {
                if (it.isEmpty()) {
                    null
                } else {
                    it.joinToString(",")
                }
            },
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

    private fun Commit.allAuthors() = setOf(committer.coAuthor(), author.coAuthor()) + coAuthors()

    private fun Person.coAuthor() = CoAuthor(name, email)

    private fun Commit.coAuthors() = fullMessage.split("\n")
        .mapNotNull { it.lineCoAuthor() }

    private fun String.lineCoAuthor() = coAuthorRegex.matchEntire(this)
        ?.groupValues
        ?.let { (_, name, email) -> CoAuthor(name, email) }
}

private fun Commit.ease(): Int? = runCatching { easeRegex.matchEntire(fullMessage)?.groups?.get("ease") }
    .getOrNull()
    ?.value
    ?.toIntOrNull()

private fun Commit.storyId(): String? = runCatching { storyRegex.matchEntire(fullMessage)?.groups?.get("storyId") }
    .getOrNull()
    ?.value

data class CoAuthor(
    val name: String,
    val email: String,
)
