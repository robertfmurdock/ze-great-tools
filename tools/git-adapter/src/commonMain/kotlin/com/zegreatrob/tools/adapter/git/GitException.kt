package com.zegreatrob.tools.adapter.git

sealed class GitException(
    message: String,
    open val command: String,
    open val exitCode: Int,
    open val stderr: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isPermissionError: Boolean
        get() = exitCode in listOf(128, 403) || stderr.contains("permission", ignoreCase = true)

    fun toUserMessage(): String = buildString {
        appendLine("Command failed: $command (exit code $exitCode)")
        if (stderr.isNotBlank()) {
            appendLine()
            appendLine("Error output:")
            stderr.lines().forEach { line ->
                appendLine("  $line")
            }
        }

        if (isPermissionError && command.contains("push")) {
            appendLine()
            appendLine("The account running this command needs push permission on the remote repository.")
            when {
                stderr.contains("TF401027") || stderr.contains("Azure DevOps") -> {
                    appendLine("For Azure DevOps, grant 'Contribute' and 'Create tag' permissions to the Build Service")
                    appendLine("identity in repo Security settings.")
                }

                stderr.contains("GitHub") || stderr.contains("github.com") -> {
                    appendLine("For GitHub Actions, ensure 'permissions: contents: write' is set on the job.")
                }

                else -> {
                    appendLine("Check that the user has appropriate permissions in the repository settings.")
                }
            }
        }
    }
}

class GitRepositoryNotFoundException(
    override val command: String,
    override val exitCode: Int,
    override val stderr: String,
    cause: Throwable? = null,
) : GitException(
    message = "Not a git repository: $command (exit code $exitCode): $stderr",
    command = command,
    exitCode = exitCode,
    stderr = stderr,
    cause = cause,
)

class GitTagAlreadyExistsException(
    val tagName: String,
    override val command: String,
    override val exitCode: Int,
    override val stderr: String,
    cause: Throwable? = null,
) : GitException(
    message = "Tag '$tagName' already exists: $command (exit code $exitCode): $stderr",
    command = command,
    exitCode = exitCode,
    stderr = stderr,
    cause = cause,
)

class GitInvalidRefException(
    val ref: String,
    override val command: String,
    override val exitCode: Int,
    override val stderr: String,
    cause: Throwable? = null,
) : GitException(
    message = "Invalid git ref '$ref': $command (exit code $exitCode): $stderr",
    command = command,
    exitCode = exitCode,
    stderr = stderr,
    cause = cause,
)

class GitUncommittedChangesException(
    override val command: String,
    override val exitCode: Int,
    override val stderr: String,
    cause: Throwable? = null,
) : GitException(
    message = "Working directory has uncommitted changes: $command (exit code $exitCode): $stderr",
    command = command,
    exitCode = exitCode,
    stderr = stderr,
    cause = cause,
)

class GitCommandExecutionException(
    override val command: String,
    override val exitCode: Int,
    override val stderr: String,
    cause: Throwable? = null,
) : GitException(
    message = "Git command execution failed: $command (exit code $exitCode): $stderr",
    command = command,
    exitCode = exitCode,
    stderr = stderr,
    cause = cause,
)

fun parseGitError(command: String, exitCode: Int, stderr: String, cause: Throwable? = null): GitException = when {
    stderr.contains("not a git repository", ignoreCase = true) ||
        stderr.contains("must be run in a work tree", ignoreCase = true) ->
        GitRepositoryNotFoundException(command, exitCode, stderr, cause)

    stderr.contains("already exists", ignoreCase = true) &&
        (stderr.contains("tag '", ignoreCase = true) || command.contains("tag")) -> {
        val tagMatch = "tag '([^']+)' already exists".toRegex(RegexOption.IGNORE_CASE).find(stderr)
            ?: "'([^']+)' already exists".toRegex(RegexOption.IGNORE_CASE).find(stderr)
        val tagName = tagMatch?.groupValues?.get(1) ?: ""
        GitTagAlreadyExistsException(tagName, command, exitCode, stderr, cause)
    }

    stderr.contains("unknown revision", ignoreCase = true) ||
        stderr.contains("ambiguous argument", ignoreCase = true) ||
        stderr.contains("invalid refspec", ignoreCase = true) ||
        stderr.contains("Needed a single revision", ignoreCase = true) ||
        stderr.contains("bad revision", ignoreCase = true) -> {
        val refMatch = "ambiguous argument '([^']+)':".toRegex(RegexOption.IGNORE_CASE).find(stderr)
            ?: "unknown revision or path not in the working tree: '([^']+)'".toRegex(RegexOption.IGNORE_CASE).find(stderr)
            ?: "bad revision '([^']+)'".toRegex(RegexOption.IGNORE_CASE).find(stderr)
        val ref = refMatch?.groupValues?.get(1) ?: ""
        GitInvalidRefException(ref, command, exitCode, stderr, cause)
    }

    stderr.contains("commit or stash", ignoreCase = true) ||
        stderr.contains("local changes", ignoreCase = true) ||
        stderr.contains("You have unstaged changes", ignoreCase = true) ||
        stderr.contains("Please commit your changes", ignoreCase = true) ->
        GitUncommittedChangesException(command, exitCode, stderr, cause)

    else -> GitCommandExecutionException(command, exitCode, stderr, cause)
}

fun parseGitError(error: ProcessError): GitException = parseGitError(error.command, error.exitCode, error.stderr, error)
