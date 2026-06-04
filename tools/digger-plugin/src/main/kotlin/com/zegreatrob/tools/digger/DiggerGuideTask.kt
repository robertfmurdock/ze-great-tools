package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.guide.getDiggerGuideContent
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Prints help output to console, no file outputs to cache")
abstract class DiggerGuideTask : DefaultTask() {

    @Internal
    fun getGuideContent(): String = getDiggerGuideContent()

    @TaskAction
    fun execute() {
        val content = getGuideContent()
        println(formatGuideForConsole(content))
    }

    private fun formatGuideForConsole(markdown: String): String = """
            ╔═══════════════════════════════════════════════════════════╗
            ║           Digger Plugin - Usage Guide                     ║
            ╚═══════════════════════════════════════════════════════════╝

            $markdown

            AVAILABLE TASKS
            ───────────────
            • gitHead                   - Display HEAD commit info (read-only)
            • currentContributionData   - Analyze current commit (read-only)
            • allContributionData       - Analyze all history (read-only)

            DOCUMENTATION
            ─────────────
            For detailed configuration and examples, see:
            https://github.com/robertfmurdock/ze-great-tools/tree/main/tools/digger-plugin
    """.trimIndent()
}
