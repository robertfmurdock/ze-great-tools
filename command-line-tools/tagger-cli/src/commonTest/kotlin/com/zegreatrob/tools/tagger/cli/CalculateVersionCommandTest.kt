package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlin.test.BeforeTest

class CalculateVersionCommandTest : CalculateVersionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "calculate-version")
    }

    override fun configureWithDefaults() {
        arguments += "--release-branch=master"
        arguments += projectDir
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
        implicitPatch?.let { arguments += "--implicit-patch=$implicitPatch" }
        disableDetached?.let { arguments += "--disable-detached=$disableDetached" }
        versionRegex?.let { arguments += "--version-regex=$versionRegex" }
        majorRegex?.let { arguments += "--major-regex=$majorRegex" }
        minorRegex?.let { arguments += "--minor-regex=$minorRegex" }
        patchRegex?.let { arguments += "--patch-regex=$patchRegex" }
        noneRegex?.let { arguments += "--none-regex=$noneRegex" }
        forceSnapshot?.let { arguments += "--force-snapshot=$forceSnapshot" }
        arguments += "--release-branch=master"
        arguments += projectDir
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(arguments)
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
