package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.digger.CurrentContributionTestSpec
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class CurrentContributionDataTest : CurrentContributionTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    private lateinit var outputFile: File

    override val addFileNames: Set<String> = emptySet()

    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = emptyList()
        outputFile = projectDir.resolve("temp-file.json")
    }

    override fun setupWithDefaults() {
        arguments += "--output-file=${outputFile.absolutePath}"
        arguments += projectDir.absolutePath
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
            .let { assertEquals("Data written to ${outputFile.absolutePath}\n", it) }
        return outputFile.readText()
    }
}
