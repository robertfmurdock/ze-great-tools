package com.zegreatrob.tools.digger

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.operation.LogOp

val coAuthorRegex = Regex("Co-authored-by: (.*) <(.*)>", RegexOption.IGNORE_CASE)

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun currentContributionData(): Set<CoAuthor> = run {
        val grgit = grgitServiceExtension.service.get().grgit

        recentCommits(grgit)
            .flatMap { it.allAuthors() }.toSet()
    }

    private fun recentCommits(grgit: Grgit): List<Commit> {
        val tag = grgit.previousTag()
        return if (tag == null) {
            grgit.log()
        } else {
            return grgit.log(fun(it: LogOp) { it.range(tag, "HEAD") })
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

data class CoAuthor(
    val name: String,
    val email: String,
)
