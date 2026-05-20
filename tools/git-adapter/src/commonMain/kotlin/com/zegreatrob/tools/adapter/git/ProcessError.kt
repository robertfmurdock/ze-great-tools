package com.zegreatrob.tools.adapter.git

data class ProcessError(
    val exitCode: Int,
    val stderr: String,
    val command: String,
) : Exception("Process failed with exit code $exitCode") {
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
