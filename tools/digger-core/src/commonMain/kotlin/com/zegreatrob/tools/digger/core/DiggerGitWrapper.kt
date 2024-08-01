package com.zegreatrob.tools.digger.core

import kotlinx.datetime.Instant

class DiggerGitWrapper(private val workingDirectory: String) {

    fun headCommitId(): String {
        val outputText = runProcess(
            listOf(
                "git",
                "--no-pager",
                "rev-parse",
                "HEAD",
            ),
            workingDirectory,
        )
        return outputText.trim()
    }

    fun listTags(): List<TagRef> {
        val outputText = runProcess(
            listOf(
                "git",
                "--no-pager",
                "tag",
                "--list",
                "--format=%(refname:strip=2),%(*objectname),%(creatordate:iso-strict)",
            ),
            workingDirectory,
        )
        val output = outputText.split("\n").mapNotNull {
            val commaSplit = it.split(",")
            if (commaSplit.size >= 3) {
                TagRef(
                    name = commaSplit.subList(0, commaSplit.size - 2).joinToString(""),
                    commitId = commaSplit.reversed()[1],
                    dateTime = Instant.parse(commaSplit.last()),
                )
            } else {
                null
            }
        }
        return output
    }

    fun log(): List<CommitRef> = parseLog(
        runProcess(
            listOf(
                "git",
                "--no-pager",
                "log",
                "--format=%H%n%ae%n%ce%n%aI%n%B%n$commitSeparator",
            ),
            workingDirectory,
        ),
    )

    fun logWithRange(begin: String, end: String): List<CommitRef> = parseLog(
        runProcess(
            listOf(
                "git",
                "--no-pager",
                "log",
                "--format=%H%n%ae%n%ce%n%aI%n%B%n$commitSeparator",
                "$begin..$end",
            ),
            workingDirectory,
        ),
    )

    private val commitSeparator = "--------!--------"

    private fun parseLog(outputText: String) = outputText.split("$commitSeparator\n")
        .filter { it.isNotEmpty() }
        .map { commitEntry ->
            val elements = commitEntry.split("\n")
            CommitRef(
                id = elements[0],
                authorEmail = elements[1],
                committerEmail = elements[2],
                dateTime = Instant.parse(elements[3]),
                fullMessage = elements.subList(4, elements.size).joinToString("\n"),
            )
        }
}

expect fun runProcess(args: List<String>, workingDirectory: String): String
