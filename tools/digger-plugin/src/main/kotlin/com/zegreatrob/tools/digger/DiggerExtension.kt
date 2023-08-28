package com.zegreatrob.tools.digger

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.operation.LogOp

val coAuthorRegex = Regex("Co-authored-by: (.*) <(.*)>")

open class DiggerExtension(
    private val grgitServiceExtension: GrgitServiceExtension,
) {

    fun collectCoAuthors(): Set<CoAuthor> = run {
        val grgit = grgitServiceExtension.service.get().grgit

        recentCommits(grgit)
            .flatMap { it.allAuthors() }.toSet()
    }

    private fun recentCommits(grgit: Grgit): List<Commit> {
        val description = grgit.describe {}
        val descriptionComponents = description?.split("-")
        val previousTag = descriptionComponents?.getOrNull(0)

        return if (previousTag == null) {
            grgit.log()
        } else {
            return grgit.log(fun(it: LogOp) { it.range(previousTag, "HEAD") })
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
