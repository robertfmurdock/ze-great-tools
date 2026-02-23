package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.digger.CurrentContributionTestSpec

class CurrentContributionDataTest : CurrentContributionTestSpec {

    override lateinit var projectDir: String
    private val outputFile: String get() = "$projectDir/temp-file.json"
    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>

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
        arguments = listOf(
            "--output-file=$outputFile",
            projectDir,
        ) + listOfNotNull(
            label?.let { "--label=$it" },
            majorRegex?.let { "--major-regex=$it" },
            minorRegex?.let { "--minor-regex=$it" },
            patchRegex?.let { "--patch-regex=$it" },
            noneRegex?.let { "--none-regex=$it" },
            storyRegex?.let { "--story-id-regex=$it" },
            easeRegex?.let { "--ease-regex=$it" },
            tagRegex?.let { "--tag-regex=$it" },
        )
    }

    override fun runCurrentContributionData(): String {
        CurrentContributionData().test(arguments).output
            .let { it.assertIsEqualTo("Data written to ${outputFile}\n") }
        return readFromFile(outputFile) ?: ""
    }
}
