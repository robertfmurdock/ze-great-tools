package com.zegreatrob.tools.tagger

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Prints help output to console, no file outputs to cache")
abstract class TaggerGuideTask : DefaultTask() {
    @TaskAction
    fun execute() {
        println(
            """
            ╔═══════════════════════════════════════════════════════════╗
            ║           Tagger Plugin - Usage Guide                     ║
            ╚═══════════════════════════════════════════════════════════╝

            USE WHEN
            --------
            • You need automatic semantic versioning from conventional commits
            • You want to enforce version consistency across builds
            • You're working in a CI environment with Git history
            • You need to create Git tags and GitHub releases automatically

            DON'T USE WHEN
            ---------------
            • You need manual version control
            • Your project doesn't use conventional commits
            • You're in a shallow Git clone (CI must fetch full history)

            TYPICAL USAGE
            ─────────────
            ┌─────────────────────────────────────────────────────────┐
            │ # Calculate next version (read-only)                    │
            │ ./gradlew calculateVersion                              │
            │                                                          │
            │ # Create tag after successful build                     │
            │ ./gradlew check release                                 │
            └─────────────────────────────────────────────────────────┘

            BEST PRACTICES
            ──────────────
            DO:
            • Run calculateVersion before tagging to verify version
            • Use [major]/[minor]/[patch]/[none] in commit messages
            • Run 'check' task before 'release' to ensure quality
            • Configure releaseBranch to match your workflow (default: main)

            DON'T:
            • Tag manually when using the plugin (use 'release' task)
            • Run 'tag' task directly (use 'release' instead)
            • Skip version calculation step in CI pipelines

            WORKFLOW
            ────────
            1. Write code and commit with conventional commit messages
            2. Run calculateVersion to preview next version
            3. Run check task to validate your build
            4. Run release task to tag and optionally publish
            5. Plugin creates Git tag and optional GitHub release

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
            """.trimIndent(),
        )
    }
}
