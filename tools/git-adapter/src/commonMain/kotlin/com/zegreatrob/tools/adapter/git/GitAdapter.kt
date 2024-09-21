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

    fun newAnnotatedTag(name: String, ref: String, userName: String?, userEmail: String?) {
        val command = listOf("git") + inlineConfig("user.name", userName) + inlineConfig("user.email", userEmail) +
            listOf(
                "tag",
                "--annotate",
                "--message=$name",
                name,
                ref,
            )
        runProcess(
            command,
            workingDirectory,
        )
    }

    private fun inlineConfig(property: String, value: String?) =
        if (value != null) listOf("-c", "$property=$value") else emptyList()

    fun pushTags() {
        runProcess(listOf("git", "push", "--tags"), workingDirectory)
    }

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
        return parseTagRefs(outputText)
    }

    private fun parseTagRefs(outputText: String): List<TagRef> {
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

    fun showTag(ref: String): TagRef? = runProcess(
        listOf(
            "git",
            "--no-pager",
            "tag",
            "--list",
            "--format=%(refname:strip=2),%(*objectname),%(creatordate:iso-strict)",
            "--points-at=$ref",
        ),
        workingDirectory,
    ).let(::parseTagRefs)
        .firstOrNull()

    fun show(ref: String): CommitRef? = parseLog(
        runProcess(
            listOf(
                "git",
                "show",
                ref,
                "--format=$gitLogFormat",
                "--no-patch",
            ),
            workingDirectory,
        ),
    ).firstOrNull()

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
        .filter(String::isNotBlank)
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
    )
        .let { output ->
            val lines = output.split("\n")
            val head = lines.findByPrefix("# branch.head ")
            val upstream = lines.findByPrefix("# branch.upstream ")
            val (a, b) = lines.findByPrefix("# branch.ab ")
                ?.split(" ")
                ?.map { it.substring(1) }
                ?: listOf("-1", "-1")
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

    private fun List<String>.findByPrefix(prefix: String) =
        find { it.startsWith(prefix) }?.substring(prefix.length)

    fun describe(abbrev: Int): String? = runCatching {
        runProcess(listOf("git", "describe", "--abbrev=$abbrev"), workingDirectory)
            .trim()
    }.getOrNull()

    fun init() {
        runProcess(listOf("git", "init"), workingDirectory)
    }

    fun config(name: String, value: String) {
        runProcess(listOf("git", "config", name, value), workingDirectory)
    }

    fun add(vararg files: String) {
        runProcess(listOf("git", "add") + files, workingDirectory)
    }

    fun commit(
        message: String,
        authorName: String,
        authorEmail: String,
        committerName: String,
        committerEmail: String,
    ) {
        runProcess(
            args = listOf(
                "git",
                "commit",
                "--message=$message",
                "--allow-empty",
            ),
            env = mapOf(
                "GIT_AUTHOR_NAME" to authorName,
                "GIT_AUTHOR_EMAIL" to authorEmail,
                "GIT_COMMITTER_NAME" to committerName,
                "GIT_COMMITTER_EMAIL" to committerEmail,
            ),
            workingDirectory = workingDirectory,
        )
    }

    fun addRemote(name: String, url: String) {
        runProcess(listOf("git", "remote", "add", name, url), workingDirectory)
    }

    fun fetch() {
        runProcess(listOf("git", "fetch"), workingDirectory)
    }

    fun merge(branch: String, noCommit: Boolean) {
        runProcess(
            listOf("git", "merge") + (if (noCommit) listOf("--no-commit") else emptyList()) + listOf(branch),
            workingDirectory,
        )
    }
}

data class GitStatus(
    val isClean: Boolean,
    val ahead: Int,
    val behind: Int,
    val head: String,
    val upstream: String,
)
