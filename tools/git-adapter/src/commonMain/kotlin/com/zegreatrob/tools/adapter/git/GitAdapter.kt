package com.zegreatrob.tools.adapter.git

import kotlinx.datetime.Instant

class GitAdapter(private val workingDirectory: String, private val env: Map<String, String> = emptyMap()) {

    fun headCommitId(): String = runProcess(
        listOf(
            "git",
            "--no-pager",
            "rev-parse",
            "HEAD",
        ),
    ).trim()

    private fun runProcess(args: List<String>, env: Map<String, String> = emptyMap()) =
        runProcess(args, workingDirectory, env.plus(this.env))

    fun newAnnotatedTag(name: String, ref: String, userName: String?, userEmail: String?) {
        runProcess(
            listOf("git") + inlineConfig("user.name", userName) + inlineConfig("user.email", userEmail) +
                listOf(
                    "tag",
                    "--annotate",
                    "--message=$name",
                    name,
                    ref,
                ),
            env = emptyMap<String, String>()
                .plus(
                    if (userName != null) {
                        mapOf("GIT_COMMITTER_NAME" to userName)
                    } else {
                        emptyMap()
                    },
                )
                .plus(
                    if (userEmail != null) {
                        mapOf("GIT_COMMITTER_EMAIL" to userEmail)
                    } else {
                        emptyMap()
                    },
                ),
        )
    }

    private fun inlineConfig(property: String, value: String?) =
        if (value != null) listOf("-c", "$property=$value") else emptyList()

    fun pushTags() {
        runProcess(listOf("git", "push", "--tags"))
    }

    fun listTags(): List<TagRef> = parseTagRefs(
        runProcess(
            listOf(
                "git",
                "--no-pager",
                "tag",
                "--list",
                "--sort=-taggerdate",
                "--format=%(refname:strip=2),%(*objectname),%(creatordate:iso-strict)",
            ),
        ),
    )

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
        runProcess(listOf("git", "describe", "--abbrev=$abbrev"))
            .trim()
    }.getOrNull()

    fun init() {
        runProcess(listOf("git", "init"))
    }

    fun config(name: String, value: String) {
        runProcess(listOf("git", "config", name, value))
    }

    fun add(vararg files: String) {
        runProcess(listOf("git", "add") + files)
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
        )
    }

    fun addRemote(name: String, url: String) {
        runProcess(listOf("git", "remote", "add", name, url))
    }

    fun fetch() {
        runProcess(listOf("git", "fetch"))
    }

    fun merge(branch: String, noCommit: Boolean, ffOnly: Boolean) {
        runProcess(
            listOf("git", "merge") + inlineFlag("--no-commit", noCommit) + inlineFlag("--ff-only", ffOnly) + listOf(
                branch,
            ),
        )
    }

    private fun inlineFlag(flag: String, enabled: Boolean) = (if (enabled) listOf(flag) else emptyList())

    fun checkout(branch: String, newBranch: Boolean = false) {
        runProcess(
            listOf("git", "checkout") + (if (newBranch) listOf("-b") else emptyList()) + listOf(
                branch,
            ),
        )
    }

    fun push(force: Boolean = false, upstream: String? = null, branch: String? = null) {
        runProcess(
            listOf(
                "git",
                "push",
            ) + (if (force) listOf("-f") else emptyList()) + (
                if (upstream != null) {
                    listOf(
                        "--set-upstream",
                        upstream,
                    )
                } else {
                    emptyList()
                }
                ) + if (branch != null) {
                listOf(branch)
            } else {
                emptyList()
            },
        )
    }

    fun setBranchUpstream(upstream: String, branch: String) {
        runProcess(listOf("git", "branch", "--set-upstream-to=$upstream", branch))
    }
}
