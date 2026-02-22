package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
class CalculateVersionCommandTest : CalculateVersionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var baseArguments: List<String>

    override fun configureWithDefaults() {
        baseArguments = listOf(
            "-q",
            "calculate-version",
            "--release-branch=master",
            projectDir,
        )
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        disableDetached: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
    ) {
        baseArguments = listOf("-q", "calculate-version") +
            listOfNotNull(
                implicitPatch?.let { "--implicit-patch=$it" },
                disableDetached?.let { "--disable-detached=$it" },
                versionRegex?.let { "--version-regex=$it" },
                majorRegex?.let { "--major-regex=$it" },
                minorRegex?.let { "--minor-regex=$it" },
                patchRegex?.let { "--patch-regex=$it" },
                noneRegex?.let { "--none-regex=$it" },
                forceSnapshot?.let { "--force-snapshot=$it" },
            ) +
            listOf("--release-branch=master", projectDir)
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(baseArguments)
        return if (test.statusCode == 0) {
            TestResult.Success(
                message = test.stdout.trim(),
                details = test.stderr.trim(),
            )
        } else {
            TestResult.Failure(test.output.trim())
        }
    }
}
