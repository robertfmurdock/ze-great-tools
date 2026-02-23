package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.digger.AllContributionTestSpec

class AllContributionDataTest : AllContributionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>
    private val outputFile: String get() = "$projectDir/temp-file.json"

    override fun setupWithDefaults() {
        arguments = listOf(
            "--output-file=$outputFile",
            projectDir,
        )
    }

    override fun setupWithOverrides(
        label: String?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        noneRegex: String?,
        storyRegex: String?,
        easeRegex: String?,
        tagRegex: String?,
    ) {
        val majorEntries = listOfNotNull(
            majorRegex?.let { "--major-regex=$it" },
            majorRegex?.let { "--major-regex=$it" },
        )
        arguments = listOf(
            "--output-file=$outputFile",
            projectDir,
        ) + listOfNotNull(
            label?.let { "--label=$it" },
            minorRegex?.let { "--minor-regex=$it" },
            patchRegex?.let { "--patch-regex=$it" },
            noneRegex?.let { "--none-regex=$it" },
            storyRegex?.let { "--story-id-regex=$it" },
            easeRegex?.let { "--ease-regex=$it" },
            tagRegex?.let { "--tag-regex=$it" },
        ) + majorEntries
    }

    override fun runAllContributionData(): AllContributionTestSpec.AllContributionDataResult {
        val output = AllContributionData()
            .test(arguments)
            .output
        return AllContributionTestSpec.AllContributionDataResult(
            output = output,
            data = readFromFile(outputFile) ?: "",
        )
    }

    override fun verifyOutput(result: AllContributionTestSpec.AllContributionDataResult) {
        result.output.assertIsEqualTo("Data written to ${outputFile}\n")
    }
}
