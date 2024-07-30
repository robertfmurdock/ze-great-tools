package com.zegreatrob.tools.digger.core

import kotlinx.datetime.Instant
import java.io.File
import java.nio.charset.Charset

class DiggerGitWrapper(private val workingDirectory: File) {

    fun headCommitId(): String {
        val outputText = runProcess(
            listOf(
                "git",
                "--no-pager",
                "rev-parse",
                "HEAD",
            ),
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

    private fun runProcess(args: List<String>): String {
        val process = ProcessBuilder(args)
            .directory(workingDirectory)
            .start()
        val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
        val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
        process.waitFor()
        if (error.isNotEmpty()) {
            throw Error(error)
        }
        return outputText
    }

    fun log(): List<CommitRef> = parseLog(
        runProcess(
            listOf(
                "git",
                "--no-pager",
                "log",
                "--format=%H%n%ae%n%ce%n%aI%n%B%n$commitSeparator",
            ),
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
