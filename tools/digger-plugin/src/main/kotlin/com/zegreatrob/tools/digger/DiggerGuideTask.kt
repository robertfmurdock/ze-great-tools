package com.zegreatrob.tools.digger

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Prints help output to console, no file outputs to cache")
abstract class DiggerGuideTask : DefaultTask() {
    @TaskAction
    fun execute() {
        println(
            """
            ╔═══════════════════════════════════════════════════════════╗
            ║           Digger Plugin - Usage Guide                     ║
            ╚═══════════════════════════════════════════════════════════╝

            USE WHEN
            --------
            • You need to analyze Git contributions for attribution
            • You want to track code ownership and contribution patterns
            • You need to generate contribution reports for releases
            • You're building tools that depend on Git authorship data

            DON'T USE WHEN
            ---------------
            • You're in a shallow Git clone (requires full history)
            • You need real-time contribution tracking (Digger analyzes snapshots)
            • Your repository is empty or lacks Git history

            TYPICAL USAGE
            ─────────────
            ┌─────────────────────────────────────────────────────────┐
            │ # Analyze contributions for current commit              │
            │ ./gradlew currentContributionData                       │
            │                                                          │
            │ # Analyze contributions across all history              │
            │ ./gradlew allContributionData                           │
            │                                                          │
            │ # Display current HEAD commit information               │
            │ ./gradlew gitHead                                       │
            └─────────────────────────────────────────────────────────┘

            BEST PRACTICES
            ──────────────
            DO:
            • Ensure full Git history is available (avoid shallow clones)
            • Use currentContributionData for per-commit analysis
            • Use allContributionData for comprehensive repository reports
            • Export to GitHub environment when running in CI (exportToGithub=true)

            DON'T:
            • Run analysis on shallow clones (fetch full history first)
            • Rely on Digger for real-time contribution metrics
            • Assume contribution data includes uncommitted changes

            PREREQUISITES
            ─────────────
            • Full Git repository (not a shallow clone)
            • Git must be available in PATH
            • Repository must have at least one commit
            • Working directory must be inside a Git repository

            WORKFLOW
            ────────
            1. Clone repository with full history (avoid --depth flag)
            2. Run currentContributionData for single commit analysis
            3. Run allContributionData for comprehensive repository scan
            4. Contribution data is output as JSON in build/digger/
            5. Use exportToGithub=true in CI to export to GitHub environment

            AVAILABLE TASKS
            ───────────────
            • gitHead                   - Display HEAD commit info (read-only)
            • currentContributionData   - Analyze current commit (read-only)
            • allContributionData       - Analyze all history (read-only)

            REGEX OVERRIDE CONTRACT
            ───────────────────────
            Configure Digger extension to customize contribution patterns:
            - label: Project/module identifier for output
            - workingDirectory: Git repository root path

            DOCUMENTATION
            ─────────────
            For detailed configuration and examples, see:
            https://github.com/robertfmurdock/ze-great-tools/tree/main/tools/digger-plugin
            """.trimIndent(),
        )
    }
}
