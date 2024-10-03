package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.digger.AllContributionTestSpec
import kotlin.test.BeforeTest

class AllContributionDataTest : AllContributionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>
    private val outputFile: String get() = "$projectDir/temp-file.json"

    @BeforeTest
    fun setup() {
        arguments = emptyList()
    }

    override fun setupWithDefaults() {
        arguments += "--output-file=$outputFile"
        arguments += projectDir
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
        setupWithDefaults()
        label?.let { arguments += "--label=$label" }
        majorRegex?.let { arguments += "--major-regex=$majorRegex" }
        majorRegex?.let { arguments += "--major-regex=$majorRegex" }
        minorRegex?.let { arguments += "--minor-regex=$minorRegex" }
        patchRegex?.let { arguments += "--patch-regex=$patchRegex" }
        noneRegex?.let { arguments += "--none-regex=$noneRegex" }
        storyRegex?.let { arguments += "--story-id-regex=$storyRegex" }
        easeRegex?.let { arguments += "--ease-regex=$easeRegex" }
        tagRegex?.let { arguments += "--tag-regex=$tagRegex" }
    }

    override fun runAllContributionData(): String {
        AllContributionData()
            .test(arguments)
            .output
            .let { kotlin.test.assertEquals("Data written to ${outputFile}\n", it) }
        return readFromFile(outputFile) ?: ""
    }
}
