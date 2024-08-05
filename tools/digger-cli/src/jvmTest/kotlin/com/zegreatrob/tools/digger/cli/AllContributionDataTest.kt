package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.digger.AllContributionTestSpec
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class AllContributionDataTest : AllContributionTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    override val addFileNames: Set<String> = emptySet()
    private lateinit var outputFile: File
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
    ) {
        setupWithDefaults()
        label?.let { arguments += "--label=$label" }
        majorRegex?.let { arguments += "--major-regex=$majorRegex" }
        majorRegex?.let { arguments += "--major-regex=$majorRegex" }
        minorRegex?.let { arguments += "--minor-regex=$minorRegex" }
        patchRegex?.let { arguments += "--patch-regex=$patchRegex" }
        noneRegex?.let { arguments += "--none-regex=$noneRegex" }
    }

    override fun runAllContributionData(): String {
        AllContributionData()
            .test(arguments)
            .output
            .let { assertEquals("Data written to ${outputFile.absolutePath}\n", it) }
        return outputFile.readText()
    }
}
