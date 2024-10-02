package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.createTempDirectory
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.cli.removeDirectory
import com.zegreatrob.tools.digger.CurrentContributionTestSpec
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class CurrentContributionDataTest : CurrentContributionTestSpec {

    override lateinit var projectDir: String

    private lateinit var outputFile: String

    override val addFileNames: Set<String> = emptySet()

    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        projectDir = createTempDirectory()
        arguments = emptyList()
        outputFile = "$projectDir/temp-file.json"
    }

    fun tearDown() {
        removeDirectory(projectDir)
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
        majorRegex?.let { arguments += """--major-regex=$majorRegex""" }
        minorRegex?.let { arguments += """--minor-regex=$minorRegex""" }
        patchRegex?.let { arguments += """--patch-regex=$patchRegex""" }
        noneRegex?.let { arguments += """--none-regex=$noneRegex""" }
        storyRegex?.let { arguments += """--story-id-regex=$storyRegex""" }
        easeRegex?.let { arguments += """--ease-regex=$easeRegex""" }
        tagRegex?.let { arguments += """--tag-regex=$tagRegex""" }
    }

    override fun runCurrentContributionData(): String {
        CurrentContributionData().test(arguments).output
            .let { assertEquals("Data written to ${outputFile}\n", it) }
        return readFromFile(outputFile) ?: ""
    }
}
