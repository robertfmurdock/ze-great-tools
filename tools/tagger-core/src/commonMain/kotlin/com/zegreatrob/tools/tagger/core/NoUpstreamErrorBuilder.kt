package com.zegreatrob.tools.tagger.core

/**
 * Builds an enhanced error message for the "no upstream tracking branch" condition.
 * Includes risk analysis, CI-specific guidance, and bypass warnings.
 */
fun buildNoUpstreamError(ciEnvironment: CIEnvironment = detectCIEnvironment()): String = buildString {
    appendLine("⚠️  CRITICAL CONFIGURATION ERROR")
    appendLine()
    appendLine("HEAD has no upstream tracking branch (detached HEAD state).")
    appendLine()
    appendLine("RISK:")
    appendLine("  On release branches (main, master), this can trigger unintended production releases.")
    appendLine("  Without upstream tracking, tagger cannot detect if you're ahead/behind the remote.")
    appendLine("  This may produce stable versions (1.2.3) instead of snapshots (1.2.3-SNAPSHOT),")
    appendLine("  triggering release automation (Maven Central, Docker Hub, production deploys).")
    appendLine()
    appendLine("RECOMMENDED FIX:")
    when (ciEnvironment) {
        CIEnvironment.GITHUB_ACTIONS -> {
            appendLine("  Update your GitHub Actions checkout step:")
            appendLine()
            appendLine("    - uses: actions/checkout@v4")
            appendLine("      with:")
            appendLine("        ref: \${{ github.head_ref || github.ref }}")
            appendLine("        fetch-depth: 0")
        }

        CIEnvironment.GITLAB_CI -> {
            appendLine("  Update your GitLab CI configuration:")
            appendLine()
            appendLine("    variables:")
            appendLine("      GIT_STRATEGY: clone")
            appendLine("      GIT_DEPTH: 0")
        }

        CIEnvironment.AZURE_DEVOPS -> {
            appendLine("  Update your Azure DevOps pipeline checkout:")
            appendLine()
            appendLine("    - checkout: self")
            appendLine("      fetchDepth: 0")
        }

        CIEnvironment.UNKNOWN -> {
            appendLine("  Configure your CI to:")
            appendLine("  1. Check out the actual branch (not a detached commit or merge commit)")
            appendLine("  2. Fetch full git history (not shallow clone)")
        }
    }
    appendLine()
    appendLine("BYPASS (USE WITH CAUTION):")
    appendLine("  Set allowDetachedHead = true in your tagger configuration.")
    appendLine("  ⚠️  WARNING: On release branches, this removes the safety check that prevents")
    appendLine("  accidental production releases. Only use for feature branches or when you fully")
    appendLine("  understand the risk.")
    appendLine()
    appendLine("See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md")
}

enum class CIEnvironment {
    GITHUB_ACTIONS,
    GITLAB_CI,
    AZURE_DEVOPS,
    UNKNOWN,
}

/**
 * Detects the CI environment based on standard environment variables.
 * This is for error messaging only - does not change behavior.
 */
expect fun detectCIEnvironment(): CIEnvironment
