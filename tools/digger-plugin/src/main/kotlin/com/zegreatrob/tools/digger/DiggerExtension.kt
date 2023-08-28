package com.zegreatrob.tools.digger

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.gradle.api.tasks.Input

val coAuthorRegex = Regex("Co-authored-by: (.*) <(.*)>")

open class DiggerExtension(
    val grgitServiceExtension: GrgitServiceExtension,
) {

    val coAuthors: Set<CoAuthor> by lazy {
        val grgit = grgitServiceExtension.service.get().grgit
        val commit = grgit.head()
        setOf(commit.committer.coAuthor(), commit.author.coAuthor()) + commit.coAuthors()
    }

    private fun Person.coAuthor() = CoAuthor(name, email)

    private fun Commit.coAuthors() = fullMessage.split("\n")
        .mapNotNull { it.lineCoAuthor() }

    private fun String.lineCoAuthor() = coAuthorRegex.matchEntire(this)
        ?.groupValues
        ?.let { (_, name, email) -> CoAuthor(name, email) }

    @Input
    var releaseBranch: String? = null
}

data class CoAuthor(
    val name: String,
    val email: String,
)
