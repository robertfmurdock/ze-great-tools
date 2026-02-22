package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
class TagCommandTest : TagTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var baseArguments: List<String>

    override fun configureWithDefaults() {
        baseArguments = listOf(
            "-q",
            "tag",
            "--release-branch=master",
            projectDir,
        )
    }

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        baseArguments = listOf("-q", "tag") +
            listOfNotNull(
                releaseBranch?.let { "--release-branch=$it" },
                userName?.let { "--user-name=$it" },
                userEmail?.let { "--user-email=$it" },
                warningsAsErrors?.let { "--warnings-as-errors=$it" },
            ) +
            listOf(projectDir)
    }

    override fun execute(version: String): TestResult {
        val test = cli()
            .test(baseArguments + "--version=$version")
        return if (test.statusCode == 0) {
            test
                .output
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }
}
