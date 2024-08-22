package com.zegreatrob.tools.adapter.git

import kotlinx.datetime.Instant

class GitAdapter(private val workingDirectory: String) {

    fun headCommitId(): String = runProcess(
        listOf(
            "git",
            "--no-pager",
            "rev-parse",
            "HEAD",
        ),
        workingDirectory,
    ).trim()

    fun listTags(): List<TagRef> {
        val outputText = runProcess(
            listOf(
                "git",
                "--no-pager",
                "tag",
                "--list",
                "--sort=-taggerdate",
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
                "--format=$gitLogFormat",
            ),
            workingDirectory,
        ),
    )

    fun logWithRange(begin: String, end: String? = null): List<CommitRef> = parseLog(
        runProcess(
            listOf(
                "git",
                "--no-pager",
                "log",
                "--format=$gitLogFormat",
                begin,
            ) + if (end != null) listOf(end) else emptyList(),
            workingDirectory,
        ),
    )

    private val commitSeparator = "--------!--------"
    private val gitLogFormat = "%H%n%ae%n%ce%n%aI%n%P%n%B%n$commitSeparator"

    private fun parseLog(outputText: String) = outputText
        .split("$commitSeparator\n")
        .filter { it.isNotEmpty() }
        .map { commitEntry ->
            val elements = commitEntry.split("\n")
            CommitRef(
                id = elements[0],
                authorEmail = elements[1],
                committerEmail = elements[2],
                dateTime = Instant.parse(elements[3]),
                parents = elements[4].split(" ").filter { it.isNotEmpty() },
                fullMessage = elements.subList(5, elements.size).joinToString("\n"),
            )
        }

    fun status(): GitStatus = runProcess(
        listOf(
            "git",
            "status",
            "--porcelain=2",
            "--branch",
            "--ahead-behind",
        ),
        workingDirectory,
    ).let { output ->
        val lines = output.split("\n")
        val head = statusValue(lines, "# branch.head")
        val upstream = statusValue(lines, "# branch.upstream")
        val (a, b) = statusValue(lines, "# branch.ab")?.split(" ") ?: listOf("-1", "-1")
        GitStatus(
            isClean = lines
                .filterNot { it.startsWith("#") }
                .filterNot(String::isBlank)
                .isEmpty(),
            ahead = a.toInt(),
            behind = b.toInt(),
            head = head ?: "",
            upstream = upstream ?: "",
        )
    }

    private fun statusValue(lines: List<String>, prefix: String) =
        lines.find { it.startsWith(prefix) }?.substring(prefix.length + 1)

    fun describe(abbrev: Int): String? = runCatching {
        runProcess(listOf("git", "describe", "--abbrev=$abbrev"), workingDirectory)
            .trim()
    }.getOrNull()
}

data class GitStatus(
    val isClean: Boolean,
    val ahead: Int,
    val behind: Int,
    val head: String,
    val upstream: String,
)
