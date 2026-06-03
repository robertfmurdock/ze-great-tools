package com.zegreatrob.tools.tagger

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Prints help output to console, no file outputs to cache")
abstract class TaggerGuideTask : DefaultTask() {

    @Internal
    fun getGuideContent(): String? = javaClass.getResourceAsStream("/help/tagger-guide.md")
        ?.bufferedReader()
        ?.use { it.readText() }

    @TaskAction
    fun execute() {
        val content = getGuideContent()
        if (content != null) {
            println(formatGuideForConsole(content))
        } else {
            println("Error: Guide content not found")
        }
    }

    private fun formatGuideForConsole(markdown: String): String = """
            ╔═══════════════════════════════════════════════════════════╗
            ║           Tagger Plugin - Usage Guide                     ║
            ╚═══════════════════════════════════════════════════════════╝

            $markdown

            AVAILABLE TASKS
            ───────────────
            • calculateVersion  - Preview next version (read-only)
            • previousVersion   - Show most recent tag (read-only)
            • commitReport      - Analyze commit messages (read-only)
            • tag               - Create Git tag (side effect)
            • githubRelease     - Create GitHub release (side effect)
            • release           - Full release workflow (orchestrator)

            DOCUMENTATION
            ─────────────
            For detailed configuration and examples, see:
            https://github.com/robertfmurdock/ze-great-tools/tree/main/tools/tagger-plugin
    """.trimIndent()
}
